package com.example.lyzr_backend.dto.converter;

import java.sql.Timestamp;
import java.time.Instant;

import com.example.lyzr_backend.dto.request.AgentRequestDTO;
import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.enums.RequestStatus;
import com.example.lyzr_backend.service.EmployeeService;

public class DtoConverter {
    public static AgentRequest getAgentRequestFromAgentRequestDTO(AgentRequestDTO agentRequestDTO, Employee employee){
        AgentRequest request = new AgentRequest();
        request.setAgentName(agentRequestDTO.getAgent_name());
        request.setCallbackUrl(agentRequestDTO.getCallBackUrl());
        request.setCurrentStatus(RequestStatus.PENDING);
        request.setCreatedAt(Timestamp.from(Instant.now()));
        request.setUpdatedAt(Timestamp.from(Instant.now()));
        request.setEmployee(employee);
        return request;
    }
}
