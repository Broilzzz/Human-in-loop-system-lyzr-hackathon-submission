package com.example.lyzr_backend.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.dto.response.AllAgentRequestWithApprovalRoundsDTO;
import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.repository.AgentRequestRepository;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;
import com.example.lyzr_backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AgentRequestRepository agentRequestRepository;
    private final ApprovalRoundRepository approvalRoundRepository;

    public Employee getEmployeeById(String id){
        Optional<Employee> optionalEmployee =  employeeRepository.findById(id);
        if(optionalEmployee.isEmpty()) {
            throw new RuntimeException("Employee not found");
        }
        return optionalEmployee.get();
    }

    public Employee getEmployeeByEmail(String email){
        Optional<Employee> optionalEmployee =  employeeRepository.findByEmail((email));
        if(optionalEmployee.isEmpty()) {
            throw new RuntimeException("Employee not found");
        }
        return optionalEmployee.get();
    }
    
    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<AllAgentRequestWithApprovalRoundsDTO> getAllAgentRequests(String employeeId){
        // give a list of all agentRequst with their list of all approvalRounds status and id

        //get all agent request list
        List<AgentRequest> agentRequests = agentRequestRepository.findByEmployee_Id(employeeId);

        List<AllAgentRequestWithApprovalRoundsDTO> ls = new ArrayList<>();
        for(AgentRequest agentRequest : agentRequests){
            List<ApprovalRound> approvalRounds = approvalRoundRepository
                    .findAllByAgentRequest_Id(agentRequest.getId());
            ls.add(new AllAgentRequestWithApprovalRoundsDTO(agentRequest, approvalRounds));
        }

        return ls;
    }
}
