package com.example.lyzr_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Attachment;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    List<Attachment> findAllByApprovalRound_Id(String approvalRoundId);
    Optional<Attachment> findByChecksum(String checksum);
    List<Attachment> findByApprovalRound(ApprovalRound approvalRound);
}
