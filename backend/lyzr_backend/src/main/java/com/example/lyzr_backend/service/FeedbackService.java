package com.example.lyzr_backend.service;

import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.entity.Feedback;
import com.example.lyzr_backend.enums.RoundStatus;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;
import com.example.lyzr_backend.repository.EmployeeRepository;
import com.example.lyzr_backend.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final ApprovalRoundRepository approvalRoundRepository;
    private final EmployeeRepository employeeRepository;

    // Add or update feedback for a round
    public Feedback addFeedback(ApprovalRound approvalRound, Employee employee, String comment) {
        Feedback feedback = new Feedback();
        feedback.setApprovalRound(approvalRound);
        feedback.setEmployee(employee);
        feedback.setComment(comment);
        feedback.setTimestamp(new Timestamp(System.currentTimeMillis()));

        return feedbackRepository.save(feedback);
    }

    // Get feedback for a round
    public Optional<Feedback> getFeedbackByRound(ApprovalRound round) {
        Optional<Feedback> opt =  feedbackRepository.findByApprovalRound(round);
        return opt;
    }
}
