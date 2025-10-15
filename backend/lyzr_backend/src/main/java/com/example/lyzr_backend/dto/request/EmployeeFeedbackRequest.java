package com.example.lyzr_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeFeedbackRequest {
    private String employeeId;
    private String feedback;
    private String status;
    private String roundId;
}
