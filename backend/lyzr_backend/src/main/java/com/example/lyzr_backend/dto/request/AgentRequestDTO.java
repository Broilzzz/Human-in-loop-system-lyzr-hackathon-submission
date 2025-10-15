package com.example.lyzr_backend.dto.request;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequestDTO {
    private String agent_name;
    private String callBackUrl;
    private String subject;
    private String context;
    private String employeeId;
    private String deadline;
    private String agentRequestId; // Optional - if provided, append to existing request
}
