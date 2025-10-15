package com.example.lyzr_backend.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRoundSqsSenderService {

    private final ApprovalRoundRepository approvalRoundRepository;
    private final SqsClient sqsClient;

    @Value("${aws.sqs.request-queue-url}")
    private String QUEUE_URL;

     @Scheduled(fixedRate = 60000) // runs every 1 min
    @Transactional
    public void sendPendingApprovalRoundsToSqs() {
        List<ApprovalRound> pendingRounds =
                approvalRoundRepository.findPendingRoundsForProcessing();

        for (ApprovalRound round : pendingRounds) {
            try {
                // Lock the record to avoid multiple takes
                round.setLocked(true);
                approvalRoundRepository.save(round);

                // Send message to SQS
                String messageBody = "ApprovalRound ID: " + round.getId();
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .messageBody(messageBody)
                        .build());

                // Mark as in SQS
                round.setInSqs(true);
                round.setLocked(false);
                approvalRoundRepository.save(round);

            } catch (Exception e) {
                round.setLocked(false);
                approvalRoundRepository.save(round);
                System.err.println("Failed to send round " + round.getId() + " to SQS: " + e.getMessage());
            }
        }
    }
}
