package com.example.lyzr_backend.dto.request;

import com.example.lyzr_backend.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseToAgentRequest {
    private RoundStatus status;
    private String approvalRoundId;
    private String feedback;
}
