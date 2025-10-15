package com.example.lyzr_backend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.enums.RequestStatus;
import com.example.lyzr_backend.repository.AgentRequestRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentRequestService {

    private final AgentRequestRepository agentRequestRepository;

    //creates a new agent request
    public AgentRequest saveAgentRequest(AgentRequest request) {
        return agentRequestRepository.save(request);
    }

    public Optional<AgentRequest> getAgentRequestById(String id) {
        return agentRequestRepository.findById(id);
    }
}
