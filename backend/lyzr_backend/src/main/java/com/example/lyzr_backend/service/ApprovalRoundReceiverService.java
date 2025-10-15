package com.example.lyzr_backend.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Employee;
import com.example.lyzr_backend.enums.RoundStatus;
import com.example.lyzr_backend.repository.ApprovalRoundRepository;
import com.example.lyzr_backend.repository.EmployeeRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalRoundReceiverService {

    private final SqsClient sqsClient;
    private final ApprovalRoundRepository approvalRoundRepository;
    private final JavaMailSender mailSender;

    @Value("${aws.sqs.request-queue-url}")
    private String QUEUE_URL;
    
    @Value("${aws.sqs.dlq-url}")
    private String DLQ_URL;
    
    // Exponential backoff configuration
    @Value("${app.backoff.initial-delay-seconds:30}")
    private int initialDelaySeconds;
    
    @Value("${app.backoff.max-delay-seconds:3600}")
    private int maxDelaySeconds;
    
    @Value("${app.backoff.multiplier:2}")
    private double backoffMultiplier;

    @Scheduled(fixedRate = 30000) // every 30 sec - Temporarily disabled for testing
    @Transactional
    public void processMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(QUEUE_URL)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        for (Message msg : messages) {
            try {
                String approvalRoundId = msg.body().replace("ApprovalRound ID: ", "");
                Optional<ApprovalRound> roundOpt = approvalRoundRepository.findById(approvalRoundId);

                if (roundOpt.isPresent()) {
                    ApprovalRound round = roundOpt.get();

                    if (RoundStatus.PENDING == round.getStatus()) {
                        // Check if deadline has passed
                        if (isDeadlinePassed(round)) {
                            log.warn("Deadline passed for round {}. Sending to DLQ.", round.getId());
                            sendToDLQ(round);
                            round.setSqsExhausted(true);
                            round.setInSqs(false);
                            approvalRoundRepository.save(round);
                        } else {
                            // Always send email and implement exponential backoff regardless of success/failure
                            boolean emailSent = sendApprovalEmail(round);
                            
                            // Implement exponential backoff for next email
                            handleExponentialBackoff(round, emailSent);
                        }
                    } else {
                        log.info("Skipping non-pending round ID: {}", round.getId());
                    }
                }

                // Delete message after processing
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .receiptHandle(msg.receiptHandle())
                        .build());

            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage(), e);
            }
        }
    }

    private boolean sendApprovalEmail(ApprovalRound round) {
        try {
            AgentRequest agentRequest = round.getAgentRequest();
            Employee employee = round.getAgentRequest().getEmployee();
            String to = employee.getEmail();
            String subject = "Approval Request from " + agentRequest.getAgentName();

            String employeeName = employee.getFirstName() + employee.getLastName();
            String agentName = agentRequest.getAgentName();
            String context = round.getContext();
            String deadline = round.getDeadline().toString();

            String approvalUrl = "http://localhost:5174/";
            String rejectUrl = "http://localhost:5174/";
            String feedbackUrl = "http://localhost:5174/";

            String body = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <p>Dear %s,</p>
                   \s
                    <p>You have a pending approval from agent:</p>
                   \s
                    <ul>
                        <li><b>Agent Name:</b> %s</li>
                        <li><b>Subject:</b> %s</li>
                        <li><b>Deadline:</b> %s</li>
                    </ul>
                   \s
                    <p>Please take an action below:</p>
                   \s
                    <div style="margin-top: 20px;">
                        <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Approve</a>
                        <a href="%s" style="background-color: #f44336; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reject</a>
                        <a href="%s" style="background-color: #2196F3; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Only Feedback</a>
                    </div>
                   \s
                    <p style="margin-top: 30px;">
                        You will be redirected to our approval portal where you can optionally add feedback before submitting.
                    </p>
                   \s
                    <p>Regards,<br>
                    <b>Lyzr Team</b></p>
                </body>
                </html>
           \s""".formatted(employeeName, agentName, context, deadline, approvalUrl, rejectUrl, feedbackUrl);

            return sendHtmlEmail(to, subject, body);
        } catch (Exception e) {
            log.error("Failed to send approval email for round {}: {}", round.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handles exponential backoff for next email regardless of success/failure
     */
    private void handleExponentialBackoff(ApprovalRound round, boolean emailSent) {
        // Calculate exponential backoff delay for next email
        int delaySeconds = calculateBackoffDelay(round);
        
        // Update backoff multiplier for next iteration
        round.setBackoffMultiplier((int) (round.getBackoffMultiplier() * backoffMultiplier));
        round.setLastSqsSentAt(new Timestamp(System.currentTimeMillis()));
        
        approvalRoundRepository.save(round);
        
        String emailStatus = emailSent ? "sent successfully" : "failed";
        log.info("Email {} for round {}. Next email in {} seconds", 
                emailStatus, round.getId(), delaySeconds);
        
        // Schedule next email with delay
        sendToSqs(round, delaySeconds);
    }
    
    /**
     * Calculates the backoff delay in seconds using exponential backoff
     * Pattern: now, 2min, 5min, 12.5min, 31.25min, etc.
     */
    private int calculateBackoffDelay(ApprovalRound round) {
        // Use the backoffMultiplier to track the current exponential level
        int currentMultiplier = round.getBackoffMultiplier();
        
        // Calculate exponential delay: initialDelay * currentMultiplier
        int delay = (int) (initialDelaySeconds * currentMultiplier);
        
        // Cap at maximum delay
        return Math.min(delay, maxDelaySeconds);
    }
    
    /**
     * Schedules the next email by sending a delayed message to SQS
     */
    private void sendToSqs(ApprovalRound round, int delaySeconds) {
        try {
            String messageBody = "ApprovalRound ID: " + round.getId();
            
            // Calculate delay in seconds (SQS supports up to 15 minutes = 900 seconds)
            int sqsDelaySeconds = Math.min(delaySeconds, 900);
            
            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(QUEUE_URL)
                    .messageBody(messageBody)
                    .delaySeconds(sqsDelaySeconds)
                    .build();
            
            sqsClient.sendMessage(sendRequest);
            log.info("Scheduled next email for round {} with {} seconds delay", round.getId(), sqsDelaySeconds);
            
        } catch (Exception e) {
            log.error("Failed to schedule next email for round {}: {}", round.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Checks if the deadline for the approval round has passed
     */
    private boolean isDeadlinePassed(ApprovalRound round) {
        if (round.getDeadline() == null) {
            return false; // No deadline set, continue processing
        }
        
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        return currentTime.after(round.getDeadline());
    }
    
    /**
     * Sends the approval round to Dead Letter Queue (DLQ) for expired deadlines
     */
    private void sendToDLQ(ApprovalRound round) {
        try {
            String messageBody = String.format(
                "Expired ApprovalRound - ID: %s, Agent: %s, Employee: %s, Deadline: %s",
                round.getId(),
                round.getAgentRequest().getAgentName(),
                round.getAgentRequest().getEmployee().getEmail(),
                round.getDeadline()
            );
            
            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(DLQ_URL)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(sendRequest);
            log.info("Sent expired round {} to DLQ", round.getId());
            
        } catch (Exception e) {
            log.error("Failed to send round {} to DLQ: {}", round.getId(), e.getMessage(), e);
        }
    }

}
