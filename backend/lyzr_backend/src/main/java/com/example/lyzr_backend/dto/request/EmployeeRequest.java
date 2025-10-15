package com.example.lyzr_backend.dto.request;

import com.example.lyzr_backend.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {
    private String firstName;
    private String lastName;
    private String email;
    private EmployeeRole role;
    private String slackId;
}
