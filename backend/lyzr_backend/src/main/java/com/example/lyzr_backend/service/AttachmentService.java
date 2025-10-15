package com.example.lyzr_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import com.example.lyzr_backend.entity.Attachment;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void saveAttachments(ApprovalRound approvalRound, List<MultipartFile> files) {

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    // Step 1: Compute file checksum (SHA-256)
                    String checksum = generateChecksum(file.getBytes());

                    // Step 2: Check if this file already exists in DB
                    Optional<Attachment> existingAttachment = attachmentRepository.findByChecksum(checksum);
                    if (existingAttachment.isPresent()) {
                        // Reuse existing file
                        Attachment reused = new Attachment();
                        reused.setApprovalRound(approvalRound);
                        reused.setFileName(file.getOriginalFilename());
                        reused.setFileType(file.getContentType());
                        reused.setFileSize((int) file.getSize());
                        reused.setS3Url(existingAttachment.get().getS3Url());
                        reused.setChecksum(checksum);
                        reused.setUploadedAt(Timestamp.from(Instant.now()));
                        attachmentRepository.save(reused);
                        continue;
                    }

                    // Step 3: Upload new file to S3
                    String key = "approval-rounds/" + approvalRound.getId() + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
                    uploadToS3(bucketName, key, file.getInputStream(), file.getContentType());

                    // Step 4: Save metadata to DB
                    Attachment attachment = new Attachment();
                    attachment.setApprovalRound(approvalRound);
                    attachment.setFileName(file.getOriginalFilename());
                    attachment.setFileType(file.getContentType());
                    attachment.setFileSize((int) file.getSize());
                    attachment.setUploadedAt(Timestamp.from(Instant.now()));
                    attachment.setS3Url("https://" + bucketName + ".s3.amazonaws.com/" + key);
                    attachment.setChecksum(checksum);

                    attachmentRepository.save(attachment);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process file: " + file.getOriginalFilename(), e);
                }
            }
        }
    }

    private void uploadToS3(String bucket, String key, InputStream inputStream, String contentType) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, inputStream.available()));
        } catch (IOException | S3Exception e) {
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        }
    }

    private String generateChecksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate checksum", e);
        }
    }

    // ---------------------------- GET ATTACHMENT FROM ROUND ID ------------------------------------------->

    public List<String> getAttachmentUrlsByApprovalRound(ApprovalRound round) {
        List<Attachment> attachments = attachmentRepository.findByApprovalRound(round);

        // Generate pre-signed URLs for secure access
        return attachments.stream()
                .map(this::generatePresignedUrl)
                .collect(Collectors.toList());
    }

    private String generatePresignedUrl(Attachment attachment) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_SOUTH_1) // change region
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            String key = extractKeyFromUrl(attachment.getS3Url());

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30)) // link valid for 30 minutes
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for " + attachment.getFileName(), e);
        }
    }

    private String extractKeyFromUrl(String s3Url) {
        // Example: https://your-bucket.s3.amazonaws.com/approval-rounds/abc123/file.pdf
        int index = s3Url.indexOf(".amazonaws.com/");
        if (index != -1) {
            return s3Url.substring(index + ".amazonaws.com/".length());
        }
        throw new IllegalArgumentException("Invalid S3 URL: " + s3Url);
    }

    public List<Attachment> getAttachmentsByApprovalRound(String approvalRoundId){
        return attachmentRepository.findAllByApprovalRound_Id(approvalRoundId);
    }
}
