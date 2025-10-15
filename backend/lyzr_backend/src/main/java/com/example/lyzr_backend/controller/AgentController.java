package com.example.lyzr_backend.controller;


import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lyzr_backend.dto.converter.DtoConverter;
import com.example.lyzr_backend.dto.request.AgentRequestDTO;
import com.example.lyzr_backend.dto.request.EmployeeLoginRequest;
import com.example.lyzr_backend.dto.request.EmployeeRequest;
import com.example.lyzr_backend.dto.request.EmployeeResponseToAgentRequest;
import com.example.lyzr_backend.dto.response.AllAgentRequestWithApprovalRoundsDTO;
import com.example.lyzr_backend.dto.response.EmployeeLoginResponse;
import com.example.lyzr_backend.dto.response.RoundsDetailResponse;
import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.enums.RequestStatus;
import com.example.lyzr_backend.service.AgentRequestService;
import com.example.lyzr_backend.service.ApprovalRoundService;
import com.example.lyzr_backend.service.ApprovalRoundSqsSenderService;
import com.example.lyzr_backend.service.AttachmentService;
import com.example.lyzr_backend.service.EmployeeService;
import com.example.lyzr_backend.service.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5174")
public class AgentController {
    private final AgentRequestService agentRequestService;
    private final ApprovalRoundService approvalRoundService;
    private final AttachmentService attachmentService;
    private final EmployeeService employeeService;
    private final FeedbackService feedbackService;
    private final ApprovalRoundSqsSenderService approvalRoundSqsSenderService;

    // ---------------------- Employee ----------------------
    
    @PostMapping("/employee")
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeRequest employeeRequest) {
        try {
            Employee employee = new Employee();
            employee.setFirstName(employeeRequest.getFirstName());
            employee.setLastName(employeeRequest.getLastName());
            employee.setEmail(employeeRequest.getEmail());
            employee.setRole(employeeRequest.getRole());
            employee.setSlackId(employeeRequest.getSlackId());
            
            // Save employee using the repository through EmployeeService
            Employee savedEmployee = employeeService.saveEmployee(employee);
            return ResponseEntity.ok(savedEmployee);
            
        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ---------------------- AgentRequest ----------------------

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> createAgentRequest(
                @RequestPart("data") String data,
                @RequestPart(value = "files", required = false) List<MultipartFile> files
        ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            AgentRequestDTO agentRequestDTO = mapper.readValue(data, AgentRequestDTO.class);
            AgentRequest agentRequest;
            
            // Check if we need to append to existing request or create new one
            if (agentRequestDTO.getAgentRequestId() != null && !agentRequestDTO.getAgentRequestId().isEmpty()) {
                // Append to existing request
                agentRequest = agentRequestService.getAgentRequestById(agentRequestDTO.getAgentRequestId())
                    .orElseThrow(() -> new RuntimeException("AgentRequest with ID " + agentRequestDTO.getAgentRequestId() + " not found"));


                Employee oldEmployee = agentRequest.getEmployee();
                Employee newEmployee = employeeService.getEmployeeById(agentRequestDTO.getEmployeeId());
                // check if they same, if not then return status
                if(newEmployee != oldEmployee) return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Cannot append to a request belonging to another employee.");

                // check if this request if already approved
                if(agentRequest.getCurrentStatus() == RequestStatus.APPROVED || agentRequest.getCurrentStatus() == RequestStatus.REJECTED){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot append to a request already approved/rejected.");
                }
            } else {
                // Create new agent request
                Employee employee = employeeService.getEmployeeById(agentRequestDTO.getEmployeeId());
                agentRequest = DtoConverter.getAgentRequestFromAgentRequestDTO(agentRequestDTO, employee);
                agentRequest = agentRequestService.saveAgentRequest(agentRequest);
            }
            
            // Create the approval round (this will create a new round with incremented round number)
            ApprovalRound approvalRound = approvalRoundService.createRound
                    (agentRequest.getId(),
                    agentRequestDTO.getSubject(),
                    agentRequestDTO.getContext(),
                            files,
                            agentRequestDTO.getDeadline()
                    );


            approvalRoundSqsSenderService.sendPendingApprovalRoundsToSqs();

            
            // Return the agent request (existing or newly created)
            return ResponseEntity.ok(agentRequest);
            
        } catch (RuntimeException e) {
            System.err.println("Business logic error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Error creating/updating agent request: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/employee/email")
    public ResponseEntity<EmployeeLoginResponse> getEmployeeLoginResponse(@RequestBody EmployeeLoginRequest employeeLoginRequest){
        Employee employee = employeeService.getEmployeeByEmail(employeeLoginRequest.getEmployeeEmail());
        EmployeeLoginResponse response = new EmployeeLoginResponse();
        response.setEmployeeId(employee.getId());
        response.setEmployeeFirstname(employee.getFirstName());
        response.setEmployeeLastName(employee.getLastName());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/employee/requests/{id}")
    public ResponseEntity<List<AllAgentRequestWithApprovalRoundsDTO>> getAllRequestsForEmployee(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(employeeService.getAllAgentRequests(id));
    }

    // NEED TO update so that only the employee whose round it is can only get it
    @GetMapping("/rounds/{id}")
    public ResponseEntity<RoundsDetailResponse> getApprovalRoundDetail(@PathVariable String id){
        return ResponseEntity.ok(approvalRoundService.getRoundsDetailFromId(id));
    }

    @PostMapping("/employee/rounds")
    public ResponseEntity<?> employeeResponseToAgent (@RequestBody EmployeeResponseToAgentRequest employeeResponseToAgentRequest){
        return ResponseEntity.ok(approvalRoundService.employeeResponseToAgent(employeeResponseToAgentRequest));
    }

}
