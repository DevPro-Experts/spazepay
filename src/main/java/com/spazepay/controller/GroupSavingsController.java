package com.spazepay.controller;

import com.spazepay.dto.savings.*;
import com.spazepay.model.User;
import com.spazepay.service.GroupSavingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings/group")
public class GroupSavingsController {
    private static final Logger logger = LoggerFactory.getLogger(GroupSavingsController.class);

    @Autowired
    private GroupSavingsService groupSavingsService;

    @PostMapping("/create")
    public ResponseEntity<GroupPlanResponse> createGroupPlan(@AuthenticationPrincipal User user,
                                                             @Valid @RequestBody CreateGroupPlanRequest request) {
        logger.info("Creating group plan for user: {}", user.getId());
        GroupPlanResponse response = groupSavingsService.createGroupPlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<GroupMemberResponse> joinGroupPlan(@AuthenticationPrincipal User user,
                                                             @PathVariable Long groupId) {
        logger.info("User {} joining group plan: {}", user.getId(), groupId);
        GroupMemberResponse response = groupSavingsService.joinGroupPlan(user, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/join")
    public ResponseEntity<GroupMemberResponse> joinGroup(@AuthenticationPrincipal User user,
                                                         @RequestParam("token") String token) {
        logger.debug("Processing join group request for userId: {} with token: {}", user.getId(), token);
        GroupMemberResponse response = groupSavingsService.joinGroup(user, token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/contribute")
    public ResponseEntity<ContributionResponse> contributeToGroup(@AuthenticationPrincipal User user,
                                                                  @PathVariable Long groupId,
                                                                  @Valid @RequestBody ContributionRequest request) {
        logger.info("User {} contributing to group plan: {}", user.getId(), groupId);
        ContributionResponse response = groupSavingsService.contributeToGroup(user, groupId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/payout")
    public ResponseEntity<PayoutResponse> processPayout(@AuthenticationPrincipal User user,
                                                        @PathVariable Long groupId,
                                                        @Valid @RequestBody PayoutRequest request) {
        logger.info("Processing payout for group plan: {}", groupId);
        PayoutResponse response = groupSavingsService.processPayout(user, groupId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupPlanResponse> getGroupPlan(@AuthenticationPrincipal User user,
                                                          @PathVariable Long groupId) {
        logger.info("User {} viewing group plan: {}", user.getId(), groupId);
        GroupPlanResponse response = groupSavingsService.getGroupPlanDetails(user, groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupPlanResponse>> getMyGroups(@AuthenticationPrincipal User user) {
        logger.info("User {} retrieving all groups", user.getId());
        List<GroupPlanResponse> response = groupSavingsService.getMyGroups(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroupPlan(@AuthenticationPrincipal User user,
                                               @PathVariable Long groupId) {
        logger.info("User {} leaving group plan: {}", user.getId(), groupId);
        groupSavingsService.leaveGroupPlan(user, groupId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/invite")
    public ResponseEntity<Void> inviteMembers(@AuthenticationPrincipal User user,
                                              @PathVariable("groupId") Long groupId,
                                              @Valid @RequestBody InviteMembersRequest request) {
        logger.debug("Processing invite members request for groupId: {} by userId: {}", groupId, user.getId());
        groupSavingsService.inviteMembers(user, groupId, request);
        return ResponseEntity.ok().build();
    }
}