package com.example.lyzr_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoginResponse {
    private String employeeId;
    private String employeeFirstname;
    private String employeeLastName;
}
