package com.example.lyzr_backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Feedback;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallBackSqsReceiverService {

    private final SqsClient sqsClient;
    private final ApprovalRoundRepository approvalRoundRepository;
    private final FeedbackService feedbackService;

    @Value("${aws.sqs.callback-queue-url}")
    private String QUEUE_URL;

    // Example local endpoint — you can change this to your Python listener port
    @Value("${http://localhost:5001/receive}")
    private String CALLBACK_URL;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Runs every 30 seconds
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processCallbackMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(QUEUE_URL)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        for (Message msg : messages) {
            try {
                String approvalRoundId = msg.body().replace("Callback for ApprovalRound ID: ", "");
                Optional<ApprovalRound> roundOpt = approvalRoundRepository.findById(approvalRoundId);

                if (roundOpt.isPresent()) {
                    ApprovalRound round = roundOpt.get();
                    sendRoundToAgent(round);
                } else {
                    log.warn("ApprovalRound with ID {} not found in DB", approvalRoundId);
                }

                // Delete message after processing
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .receiptHandle(msg.receiptHandle())
                        .build());

            } catch (Exception e) {
                log.error("Error processing callback message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Sends the ApprovalRound data as JSON to the Python receiver
     */
    private void sendRoundToAgent(ApprovalRound round) {
        try {
            // Convert ApprovalRound to a basic JSON structure
            Optional<Feedback> feedback = feedbackService.getFeedbackByRound(round);
            String json = """
                {
                    "id": "%s",
                    "agentRequestId": "%s",
                    "roundNumber": %d,
                    "status": "%s",
                    "subject": "%s",
                    "context": "%s",
                    "deadline": "%s",
                    "feedback": "%s",
                    "employeeId": "%s"
                }
            """.formatted(
                    round.getId(),
                    round.getAgentRequest() != null ? round.getAgentRequest().getId() : "null",
                    round.getRoundNumber(),
                    round.getStatus() != null ? round.getStatus().toString() : "UNKNOWN",
                    escapeJson(round.getSubject()),
                    escapeJson(round.getContext()),
                    round.getDeadline() != null ? round.getDeadline().toString() : "null",
                    feedback.isEmpty() ? "" : feedback.get().getComment(),
                    round.getAgentRequest().getEmployee().getId()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(round.getAgentRequest().getCallbackUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Sent callback for round {} — Response: {} {}",
                    round.getId(), response.statusCode(), response.body());

        } catch (Exception e) {
            log.error("Failed to send callback for round {}: {}", round.getId(), e.getMessage());
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
