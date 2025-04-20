package com.spazepay.dto.savings;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class InviteMembersRequest {
    @NotEmpty(message = "Email list cannot be empty")
    private List<String> emails;
}