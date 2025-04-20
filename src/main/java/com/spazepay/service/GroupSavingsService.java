package com.spazepay.service;

import com.spazepay.dto.savings.*;
import com.spazepay.exception.SavingsException;
import com.spazepay.model.*;
import com.spazepay.model.enums.GroupMemberStatus;
import com.spazepay.model.enums.PlanStatus;
import com.spazepay.model.enums.TransactionType;
import com.spazepay.model.savings.*;
import com.spazepay.model.savings.GroupInvitation;
import com.spazepay.repository.*;
import com.spazepay.util.CurrencyFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupSavingsService {
    private static final Logger logger = LoggerFactory.getLogger(GroupSavingsService.class);
    private static final int MIN_GROUP_SIZE = 6;
    private static final int MIN_INSTALLMENTS = 3;
    private static final BigDecimal MIN_INSTALLMENT_AMOUNT = new BigDecimal("3000.00");

    @Autowired
    private GroupPlanRepository groupPlanRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupContributionRepository contributionRepository;

    @Autowired
    private GroupContributionInstallmentRepository installmentRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository accountTransactionRepository;

    @Autowired
    private SavingsTransactionRepository transactionRepository;

    @Autowired
    private GroupInvitationRepository groupInvitationRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public GroupPlanResponse createGroupPlan(User user, CreateGroupPlanRequest request) {
        // Validations
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date must be in the future");
        }
        if (request.getContributionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }
        if (request.getCycleCount() < MIN_GROUP_SIZE) {
            throw new IllegalArgumentException("Cycle count must be at least " + MIN_GROUP_SIZE);
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getContributionFrequency() == null || request.getContributionFrequency().trim().isEmpty()) {
            throw new IllegalArgumentException("Contribution frequency is required");
        }

        // Create plan
        GroupPlan plan = new GroupPlan();
        plan.setCreator(user);
        plan.setName(request.getName());
        plan.setContributionAmount(request.getContributionAmount());
        plan.setContributionFrequency(request.getContributionFrequency());
        plan.setStartDate(request.getStartDate());
        plan.setCycleCount(request.getCycleCount());
        plan.setStatus(PlanStatus.OPEN);
        plan.setCreatedAt(Instant.now());
        plan.setLastCompletedCycle(0);
        groupPlanRepository.save(plan);

        // Add creator as first member
        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroup(plan);
        creatorMember.setUser(user);
        creatorMember.setPayoutOrder(1);
        creatorMember.setStatus(GroupMemberStatus.ACTIVE);
        groupMemberRepository.save(creatorMember);

        // Handle creator's initial contribution
        if (request.getInitialContribution() != null && request.getInitialContribution().compareTo(BigDecimal.ZERO) > 0) {
            if (!user.getPin().equals(request.getPin())) {
                throw new IllegalArgumentException("Invalid PIN");
            }
            Account account = accountService.getAccountByUserId(user.getId());
            if (account.getBalance().compareTo(request.getInitialContribution()) < 0) {
                throw new SavingsException("INSUFFICIENT_BALANCE", "Insufficient account balance");
            }
            if (request.getInitialContribution().compareTo(MIN_INSTALLMENT_AMOUNT) < 0) {
                throw new IllegalArgumentException("Initial contribution must be at least " + MIN_INSTALLMENT_AMOUNT);
            }
            if (request.getInitialContribution().compareTo(plan.getContributionAmount()) > 0) {
                throw new IllegalArgumentException("Initial contribution cannot exceed contribution amount");
            }

            account.setBalance(account.getBalance().subtract(request.getInitialContribution()));
            accountRepository.save(account);

            GroupContributionInstallment installment = new GroupContributionInstallment();
            installment.setGroup(plan);
            installment.setUser(user);
            installment.setAmount(request.getInitialContribution());
            installment.setCycleNumber(1);
            installmentRepository.save(installment);

            // Check if total contributions meet the required amount
            BigDecimal totalContributed = getTotalContributionsForCycle(plan.getId(), user.getId(), 1);
            if (totalContributed.compareTo(plan.getContributionAmount()) >= 0) {
                GroupContribution contribution = new GroupContribution();
                contribution.setGroup(plan);
                contribution.setUser(user);
                contribution.setAmount(plan.getContributionAmount());
                contribution.setCycleNumber(1);
                contributionRepository.save(contribution);
            }
        }

        logger.info("Group plan created: {}", plan.getId());
        emailService.sendHtmlEmail(
                user.getEmail(),
                "New Group Savings Plan Created",
                "<p>Dear " + user.getFullName() + ",</p>" +
                        "<p>Your group savings plan '" + plan.getName() + "' has been created.</p>" +
                        "<p>Invite at least 5 more members to start.</p>"
        );

        return buildGroupPlanResponse(plan);
    }

    @Transactional
    public GroupMemberResponse joinGroupPlan(User user, Long groupId) {
        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new SavingsException("ALREADY_IN_GROUP", "User already in group");
        }

        long activeMembers = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMembers >= plan.getCycleCount()) {
            throw new SavingsException("GROUP_FULL", "Group is full");
        }

        GroupMember member = new GroupMember();
        member.setGroup(plan);
        member.setUser(user);
        member.setPayoutOrder((int) (activeMembers + 1));
        member.setStatus(GroupMemberStatus.ACTIVE);
        groupMemberRepository.save(member);

        logger.info("User {} joined group plan: {}", user.getId(), groupId);

        GroupMemberResponse response = new GroupMemberResponse();
        response.setMemberId(member.getId());
        response.setGroupId(member.getGroup().getId());
        response.setUserId(member.getUser().getId());
        response.setPayoutOrder(member.getPayoutOrder());
        response.setStatus(member.getStatus().name());
        return response;
    }

    @Transactional
    public GroupMemberResponse joinGroup(User user, String token) {
        logger.debug("Processing join request for userId: {} with token: {}", user.getId(), token);

        GroupInvitation invitation = groupInvitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation token"));

        if (invitation.isAccepted()) {
            throw new IllegalStateException("Invitation already accepted");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Invitation has expired");
        }

        if (!invitation.getInvitee().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "This invitation is not for you");
        }

        GroupPlan plan = invitation.getGroup();
        if (plan.getStatus() != PlanStatus.OPEN) {
            throw new IllegalStateException("Cannot join a non-open group");
        }

        long activeMembers = groupMemberRepository.countByGroupIdAndStatus(plan.getId(), GroupMemberStatus.ACTIVE);
        if (activeMembers >= plan.getCycleCount()) {
            throw new IllegalStateException("Group is full");
        }

        // Mark invitation as accepted
        invitation.setAccepted(true);
        groupInvitationRepository.save(invitation);

        // Add user as member
        GroupMember member = new GroupMember();
        member.setGroup(plan);
        member.setUser(user);
        member.setPayoutOrder((int) (activeMembers + 1));
        member.setStatus(GroupMemberStatus.ACTIVE);
        groupMemberRepository.save(member);

        logger.info("User {} joined group {} via invitation", user.getId(), plan.getId());

        GroupMemberResponse response = new GroupMemberResponse();
        response.setMemberId(member.getId());
        response.setGroupId(member.getGroup().getId());
        response.setUserId(member.getUser().getId());
        response.setPayoutOrder(member.getPayoutOrder());
        response.setStatus(member.getStatus().name());
        return response;
    }

    @Transactional(rollbackFor = SavingsException.class)
    public ContributionResponse contributeToGroup(User user, Long groupId, ContributionRequest request) {
        if (!user.getPin().equals(request.getPin())) {
            throw new IllegalArgumentException("Invalid PIN");
        }

        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));

        if (request.getCycleNumber() < 1 || request.getCycleNumber() > plan.getCycleCount()) {
            throw new IllegalArgumentException("Cycle number must be between 1 and " + plan.getCycleCount());
        }
        if (request.getCycleNumber() <= plan.getLastCompletedCycle()) {
            throw new IllegalArgumentException("Cannot contribute to a completed cycle");
        }
        if (request.getAmount().compareTo(MIN_INSTALLMENT_AMOUNT) < 0) {
            throw new IllegalArgumentException("Contribution must be at least " + MIN_INSTALLMENT_AMOUNT);
        }

        GroupMember member = (GroupMember) groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not in group"));
        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new SavingsException("INACTIVE_MEMBER", "Member is not active");
        }

        // Check user's total contributions for the cycle
        BigDecimal userTotalContributed = getTotalContributionsForCycle(groupId, user.getId(), request.getCycleNumber());
        if (userTotalContributed.add(request.getAmount()).compareTo(plan.getContributionAmount()) > 0) {
            throw new IllegalArgumentException("Total contribution cannot exceed " + plan.getContributionAmount());
        }

        Account account = accountService.getAccountByUserId(user.getId());
        if (account == null) {
            throw new IllegalArgumentException("Account not found for user");
        }
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new SavingsException("INSUFFICIENT_BALANCE", "Insufficient account balance");
        }

        // Deduct from account
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        // Record installment
        GroupContributionInstallment installment = new GroupContributionInstallment();
        installment.setGroup(plan);
        installment.setUser(user);
        installment.setAmount(request.getAmount());
        installment.setCycleNumber(request.getCycleNumber());
        installmentRepository.save(installment);

        // Check if user's total contributions meet the required amount
        userTotalContributed = userTotalContributed.add(request.getAmount());
        String message = "Installment recorded";
        Long contributionId = null;
        if (userTotalContributed.compareTo(plan.getContributionAmount()) >= 0) {
            if (!contributionRepository.existsByGroupIdAndUserIdAndCycleNumber(groupId, user.getId(), request.getCycleNumber())) {
                GroupContribution contribution = new GroupContribution();
                contribution.setGroup(plan);
                contribution.setUser(user);
                contribution.setAmount(plan.getContributionAmount());
                contribution.setCycleNumber(request.getCycleNumber());
                contribution = contributionRepository.save(contribution);
                contributionId = contribution.getId();
                message = "Contribution completed";
            }
        }

        // Calculate total contributions by all members for the cycle
        BigDecimal totalContributedForCycle = getTotalContributionsForCycle(groupId, request.getCycleNumber());

        // Record account transaction
        Transaction accountTransaction = new Transaction();
        accountTransaction.setAccount(account);
        accountTransaction.setType(TransactionType.WITHDRAWAL);
        accountTransaction.setAmount(request.getAmount());
        accountTransactionRepository.save(accountTransaction);

        // Record savings transaction
        SavingsTransaction savingsTransaction = new SavingsTransaction();
        savingsTransaction.setPlanType(com.spazepay.model.enums.SavingsType.GROUP);
        savingsTransaction.setPlanId(groupId);
        savingsTransaction.setType(TransactionType.DEPOSIT);
        savingsTransaction.setAmount(request.getAmount());
        savingsTransaction.setSource("wallet");
        savingsTransaction.setNetAmount(request.getAmount());
        transactionRepository.save(savingsTransaction);

        logger.info("Installment to group {} by user {} for cycle {}: {}", groupId, user.getId(), request.getCycleNumber(), request.getAmount());

        // Build response
        ContributionResponse response = new ContributionResponse();
        response.setContributionId(contributionId);
        response.setGroupId(groupId);
        response.setAmount(request.getAmount());
        response.setTotalContributedForCycle(totalContributedForCycle);
        response.setMessage(message);
        return response;
    }

    @Transactional
    public void inviteMembers(User creator, Long groupId, InviteMembersRequest request) {
        logger.debug("Processing invitation for groupId: {} by creatorId: {}", groupId, creator.getId());

        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));

        if (!plan.getCreator().getId().equals(creator.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Only the group creator can invite members");
        }

        if (plan.getStatus() != PlanStatus.OPEN) {
            throw new IllegalStateException("Cannot invite members to a non-open group");
        }

        long activeMembers = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMembers + request.getEmails().size() > plan.getCycleCount()) {
            throw new IllegalArgumentException("Inviting these members would exceed the group size limit");
        }

        for (String email : request.getEmails()) {
            // Check if user exists
            User invitee = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));

            // Check if already a member
            if (groupMemberRepository.existsByGroupIdAndUserId(groupId, invitee.getId())) {
                logger.info("User {} is already a member of group {}", invitee.getId(), groupId);
                continue;
            }

            // Check if already invited
            if (groupInvitationRepository.existsByGroupIdAndInviteeIdAndAcceptedFalse(groupId, invitee.getId())) {
                logger.info("User {} has already been invited to group {}", invitee.getId(), groupId);
                continue;
            }

            // Create invitation
            GroupInvitation invitation = new GroupInvitation();
            invitation.setGroup(plan);
            invitation.setInvitee(invitee);
            invitation.setToken(UUID.randomUUID().toString());
            invitation.setCreatedAt(Instant.now());
            invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS)); // Expires in 7 days
            invitation.setAccepted(false);
            groupInvitationRepository.save(invitation);

            // Send invitation email
            String joinLink = "http://localhost:8080/api/savings/group/join?token=" + invitation.getToken();
            emailService.sendHtmlEmail(
                    invitee.getEmail(),
                    "Invitation to Join Group Savings Plan: " + plan.getName(),
                    "<p>Dear " + invitee.getFullName() + ",</p>" +
                            "<p>You have been invited by " + creator.getFullName() + " to join the group savings plan '<strong>" + plan.getName() + "</strong>'.</p>" +
                            "<p><strong>Plan Details:</strong></p>" +
                            "<ul>" +
                            "<li><strong>Name:</strong> " + plan.getName() + "</li>" +
                            "<li><strong>Contribution Amount:</strong> " + CurrencyFormatter.formatCurrency(plan.getContributionAmount()) + "</li>" +
                            "<li><strong>Frequency:</strong> " + plan.getContributionFrequency() + "</li>" +
                            "<li><strong>Start Date:</strong> " + plan.getStartDate() + "</li>" +
                            "<li><strong>Cycle Count:</strong> " + plan.getCycleCount() + "</li>" +
                            "<li><strong>Current Members:</strong> " + activeMembers + "</li>" +
                            "</ul>" +
                            "<p>To join the group, please click the link below:</p>" +
                            "<p><a href='" + joinLink + "'>Join Now</a></p>" +
                            "<p>This invitation expires on " + invitation.getExpiresAt() + ".</p>" +
                            "<p>Best regards,<br>SpacePay Team</p>"
            );

            logger.info("Sent invitation to user {} for group {}", invitee.getId(), groupId);
        }
    }

    @Transactional
    public PayoutResponse processPayout(User user, Long groupId, PayoutRequest request) {
        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));

        if (!plan.getCreator().getId().equals(user.getId())) {
            throw new SavingsException("NOT_AUTHORIZED", "Only the group creator can process payouts");
        }
        int cycleNumber = request.getCycleNumber();
        if (cycleNumber != plan.getLastCompletedCycle() + 1) {
            throw new IllegalStateException("Can only process the next cycle in sequence");
        }
        long expectedContributions = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        long actualContributions = contributionRepository.countByGroupIdAndCycleNumber(groupId, cycleNumber);
        if (actualContributions < expectedContributions) {
            throw new SavingsException("INCOMPLETE_CONTRIBUTIONS", "Not all members have contributed for this cycle");
        }

        List<GroupContribution> contributions = contributionRepository.findByGroupIdAndCycleNumber(groupId, cycleNumber);
        BigDecimal totalPayout = contributions.stream()
                .map(GroupContribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = totalPayout.multiply(plan.getServiceFeeRate());
        BigDecimal netPayout = totalPayout.subtract(serviceFee);

        GroupMember recipient = groupMemberRepository.findByGroupIdAndPayoutOrder(groupId, cycleNumber)
                .orElseThrow(() -> new IllegalArgumentException("No recipient for this cycle"));

        Account recipientAccount = accountService.getAccountByUserId(recipient.getUser().getId());
        recipientAccount.setBalance(recipientAccount.getBalance().add(netPayout));
        accountRepository.save(recipientAccount);

        plan.setLastCompletedCycle(cycleNumber);
        if (cycleNumber == plan.getCycleCount()) {
            plan.setStatus(PlanStatus.COMPLETED);
        }
        groupPlanRepository.save(plan);

        logger.info("Payout processed for group {} cycle {}: {}", groupId, cycleNumber, netPayout);
        PayoutResponse response = new PayoutResponse();
        response.setGroupId(groupId);
        response.setCycleNumber(cycleNumber);
        response.setPayoutAmount(netPayout);
        response.setRecipientId(recipient.getUser().getId());
        return response;
    }

    @Transactional(readOnly = true)
    public GroupPlanResponse getGroupPlanDetails(User user, Long groupId) {
        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId());
        if (!isMember) {
            throw new SavingsException("NOT_AUTHORIZED", "Not authorized to view this group");
        }
        return buildGroupPlanResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<GroupPlanResponse> getMyGroups(User user) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(user.getId());
        List<GroupPlan> plans = memberships.stream().map(GroupMember::getGroup).collect(Collectors.toList());
        return plans.stream().map(this::buildGroupPlanResponse).collect(Collectors.toList());
    }

    @Transactional
    public void leaveGroupPlan(User user, Long groupId) {
        GroupPlan plan = groupPlanRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group plan not found"));
        GroupMember member = (GroupMember) groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not in group"));

        if (plan.getLastCompletedCycle() > 0) {
            throw new SavingsException("PAYOUTS_STARTED", "Cannot leave group after payouts have started");
        }

        long activeMembers = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        if (activeMembers <= MIN_GROUP_SIZE) {
            throw new SavingsException("GROUP_TOO_SMALL", "Cannot leave group as it would have fewer than " + MIN_GROUP_SIZE + " members");
        }

        BigDecimal leaveFee = new BigDecimal("100.00");
        Account account = accountService.getAccountByUserId(user.getId());
        if (account.getBalance().compareTo(leaveFee) < 0) {
            throw new SavingsException("INSUFFICIENT_BALANCE", "Insufficient balance to pay leave fee");
        }
        account.setBalance(account.getBalance().subtract(leaveFee));
        accountRepository.save(account);

        int leavingPayoutOrder = member.getPayoutOrder();
        groupMemberRepository.delete(member);

        List<GroupMember> higherMembers = groupMemberRepository.findByGroupIdAndPayoutOrderGreaterThan(groupId, leavingPayoutOrder);
        for (GroupMember m : higherMembers) {
            m.setPayoutOrder(m.getPayoutOrder() - 1);
            groupMemberRepository.save(m);
        }

        plan.setCycleCount(plan.getCycleCount() - 1);
        groupPlanRepository.save(plan);

        logger.info("User {} left group plan: {}", user.getId(), groupId);
    }

    private BigDecimal getTotalContributionsForCycle(Long groupId, Long userId, int cycleNumber) {
        List<GroupContributionInstallment> installments = installmentRepository.findByGroupIdAndUserIdAndCycleNumber(groupId, userId, cycleNumber);
        return installments.stream()
                .map(GroupContributionInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalContributionsForCycle(Long groupId, int cycleNumber) {
        List<GroupContributionInstallment> installments = installmentRepository.findByGroupIdAndCycleNumber(groupId, cycleNumber);
        return installments.stream()
                .map(GroupContributionInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GroupPlanResponse buildGroupPlanResponse(GroupPlan plan) {
        int lastCompletedCycle = plan.getLastCompletedCycle();
        int cycleCount = plan.getCycleCount();
        Integer currentCycle = (lastCompletedCycle < cycleCount) ? lastCompletedCycle + 1 : null;
        Integer contributionsForCurrentCycle = null;
        BigDecimal totalContributedForCurrentCycle = BigDecimal.ZERO;

        if (currentCycle != null) {
            // Count members who have fully contributed (GroupContribution records)
            contributionsForCurrentCycle = (int) contributionRepository.countByGroupIdAndCycleNumber(plan.getId(), currentCycle);
            logger.debug("Contributions for groupId: {}, cycle: {}: {}", plan.getId(), currentCycle, contributionsForCurrentCycle);
            // Sum all installments for the current cycle
            totalContributedForCurrentCycle = getTotalContributionsForCycle(plan.getId(), currentCycle);
        }

        long totalActiveMembers = groupMemberRepository.countByGroupIdAndStatus(plan.getId(), GroupMemberStatus.ACTIVE);

        GroupPlanResponse response = new GroupPlanResponse();
        response.setGroupId(plan.getId());
        response.setName(plan.getName());
        response.setStatus(plan.getStatus().name());
        response.setContributionAmount(plan.getContributionAmount());
        response.setContributionFrequency(plan.getContributionFrequency());
        response.setStartDate(plan.getStartDate());
        response.setCycleCount(plan.getCycleCount());
        response.setCreatedAt(plan.getCreatedAt());
        response.setCurrentCycle(currentCycle);
        response.setContributionsForCurrentCycle(contributionsForCurrentCycle);
        response.setTotalActiveMembers((int) totalActiveMembers);
        response.setTotalContributedForCurrentCycle(totalContributedForCurrentCycle);

        return response;
    }
}