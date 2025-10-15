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
public class CallBackSqsSenderService {

    private final ApprovalRoundRepository approvalRoundRepository;
    private final SqsClient sqsClient;

    @Value("${aws.sqs.callback-queue-url}")
    private String QUEUE_URL;

    @Scheduled(fixedRate = 60000) // every 1 min - enable when ready
    @Transactional
    public void sendCallbackRoundsToSqs() {
        List<ApprovalRound> callbackRounds = approvalRoundRepository.findCallbackEligibleRounds();

        for (ApprovalRound round : callbackRounds) {
            try {
                // Lock to prevent multiple processes
                round.setLocked(true);
                approvalRoundRepository.save(round);

                // Build SQS message
                String messageBody = "Callback for ApprovalRound ID: " + round.getId();
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .messageBody(messageBody)
                        .build());

                // Update round flags after successful send
                round.setInSqs(true);
                round.setLocked(false);
                approvalRoundRepository.save(round);

                log.info("Sent callback round {} to SQS", round.getId());

            } catch (Exception e) {
                round.setLocked(false);
                approvalRoundRepository.save(round);
                log.error("Failed to send callback round {} to SQS: {}", round.getId(), e.getMessage());
            }
        }
    }
}
