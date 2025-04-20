package com.spazepay.dto.savings;

import lombok.Data;

@Data
public class GroupMemberResponse {
    private Long memberId;
    private Long groupId;
    private Long userId;
    private int payoutOrder;
    private String status;
}