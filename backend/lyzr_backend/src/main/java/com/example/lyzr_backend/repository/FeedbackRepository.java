package com.example.lyzr_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.entity.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, String> {
    Optional<Feedback> findByApprovalRound(ApprovalRound approvalRound);
}
