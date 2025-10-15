package com.example.lyzr_backend.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.lyzr_backend.dto.request.EmployeeResponseToAgentRequest;
import com.example.lyzr_backend.dto.response.RoundsDetailResponse;
import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Attachment;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.entity.Feedback;
import com.example.lyzr_backend.enums.RequestStatus;
import com.example.lyzr_backend.enums.RoundStatus;
import com.example.lyzr_backend.repository.AgentRequestRepository;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;
import com.example.lyzr_backend.repository.EmployeeRepository;
import com.example.lyzr_backend.repository.FeedbackRepository;
import jakarta.mail.Multipart;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalRoundService {

    private final ApprovalRoundRepository approvalRoundRepository;
    private final AgentRequestRepository agentRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AgentRequestService agentRequestService;
    private final FeedbackService feedbackService;
    private final FeedbackRepository feedbackRepository;
    private final AttachmentService attachmentService;


    public ApprovalRound getApprovalRound(String id){
        Optional<ApprovalRound> opt = approvalRoundRepository.findById(id);
        if(opt.isEmpty()) {
            throw new RuntimeException("ApprovlaRound not found");
        }
        return opt.get();

    }

    public ApprovalRound createRound(
            String agentRequestId,
            String contextJson,
            String subject,
            List<MultipartFile> files,
            String deadline) {
        ApprovalRound round = new ApprovalRound();
        AgentRequest agentRequest = agentRequestRepository.findById(agentRequestId)
        .orElseThrow(() -> new RuntimeException("AgentRequest not found"));

        round.setAgentRequest(agentRequest);
        ApprovalRound prevApprovalRound = approvalRoundRepository.findLatestRound(agentRequestId);
        int prevRoundNumber = prevApprovalRound==null ? 0 : prevApprovalRound.getRoundNumber();
        round.setRoundNumber(prevRoundNumber + 1);
        round.setStatus(RoundStatus.PENDING);
        round.setStartedAt(new Timestamp(System.currentTimeMillis()));
        round.setSubject(subject);
        round.setContext(contextJson);
        
        // Set deadline if provided
        if (deadline != null && !deadline.isEmpty()) {
            try {
                round.setDeadline(Timestamp.valueOf(deadline));
            } catch (Exception e) {
                // If deadline format is invalid, log error but continue
                System.err.println("Invalid deadline format: " + deadline);
            }
        }
        
        round.setInSqs(false);
        round.setLocked(false);
        round.setSqsExhausted(false);
        // Handle uploaded files (images, PDFs, etc.)
        if (files != null && !files.isEmpty()) {
            attachmentService.saveAttachments(round, files);
        }

        return approvalRoundRepository.save(round);
    }

    //get all rounds for an AgentRequest and sort them by their round number
    public List<ApprovalRound> getRoundsByAgentRequest(String agentRequestId) {
        return approvalRoundRepository.findAllByAgentRequestOrderByRoundNumberAsc(agentRequestId);
    }

    // Get the latest round
    public ApprovalRound getLatestRound(String agentRequestId) {
        return approvalRoundRepository.findLatestRound(agentRequestId);
    }

    public RoundsDetailResponse getRoundsDetailFromId(String approvalRoundId){
        ApprovalRound approvalRound =  getApprovalRound(approvalRoundId);

        RoundsDetailResponse result = new RoundsDetailResponse();
        result.setContext(approvalRound.getContext());
        result.setStatus(approvalRound.getStatus());
        result.setSubject(approvalRound.getSubject());
        result.setS3Links(attachmentService.getAttachmentUrlsByApprovalRound(approvalRound));
        result.setAgentName(approvalRound.getAgentRequest().getAgentName());

        return result;
    }

    public ResponseEntity<?> employeeResponseToAgent(EmployeeResponseToAgentRequest employeeResponseToAgentRequest){
        ApprovalRound approvalRound = getApprovalRound(employeeResponseToAgentRequest.getApprovalRoundId());
        AgentRequest agentRequest = approvalRound.getAgentRequest();

        if(employeeResponseToAgentRequest.getStatus() == RoundStatus.APPROVED){
            approvalRound.setStatus(RoundStatus.APPROVED);
            agentRequest.setCurrentStatus(RequestStatus.APPROVED);
        }else if(employeeResponseToAgentRequest.getStatus() == RoundStatus.REJECTED){
            approvalRound.setStatus(RoundStatus.REJECTED);
            agentRequest.setCurrentStatus(RequestStatus.REJECTED);
        }else if(employeeResponseToAgentRequest.getStatus() == RoundStatus.ONLY_FEEDBACK){
            approvalRound.setStatus(RoundStatus.ONLY_FEEDBACK);
            agentRequest.setCurrentStatus(RequestStatus.PENDING);
        }else{
            throw new RuntimeException("invalid status");
        }

        if(employeeResponseToAgentRequest.getFeedback() != null && !employeeResponseToAgentRequest.getFeedback().isEmpty()){
            Feedback feedback = feedbackService.addFeedback(approvalRound, agentRequest.getEmployee(), employeeResponseToAgentRequest.getFeedback());
            feedbackRepository.save(feedback);
        }
        approvalRound.setInSqs(false);
        approvalRound.setSqsExhausted(false);
        approvalRound.setLocked(false);
        approvalRound.setBackoffMultiplier(1);
        approvalRound.setCompletedAt(new Timestamp(System.currentTimeMillis()));
        approvalRoundRepository.save(approvalRound);
        agentRequestRepository.save(agentRequest);

        return ResponseEntity.noContent().build();
    }
}
