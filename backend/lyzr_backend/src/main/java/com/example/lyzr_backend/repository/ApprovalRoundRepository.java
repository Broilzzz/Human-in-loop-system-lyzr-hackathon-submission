package com.example.lyzr_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.lyzr_backend.entity.ApprovalRound;

@Repository
public interface ApprovalRoundRepository extends JpaRepository<ApprovalRound, String> {

    // Find all rounds for a specific AgentRequest by roundNumber descending
    @Query("SELECT a FROM ApprovalRound a WHERE a.agentRequest.id = :agentRequestId ORDER BY a.roundNumber DESC")
    List<ApprovalRound> findAllByAgentRequestOrderByRoundNumberDesc(@Param("agentRequestId") String agentRequestId);

    // Get the latest round
    default ApprovalRound findLatestRound(String agentRequestId) {
        List<ApprovalRound> rounds = findAllByAgentRequestOrderByRoundNumberDesc(agentRequestId);
        return rounds.isEmpty() ? null : rounds.get(0);
    }

    // Find all rounds for a specific AgentRequest by roundNumber ascending
    @Query("SELECT a FROM ApprovalRound a WHERE a.agentRequest.id = :agentRequestId ORDER BY a.roundNumber ASC")
    List<ApprovalRound> findAllByAgentRequestOrderByRoundNumberAsc(@Param("agentRequestId") String agentRequestId);

    // Return all rounds ascending (or empty list)
    default List<ApprovalRound> findAllRounds(String agentRequestId) {
        return findAllByAgentRequestOrderByRoundNumberAsc(agentRequestId);
    }

    // Finding all pending rounds (for SQS processing)
    @Query("SELECT a FROM ApprovalRound a WHERE a.inSqs = false AND a.sqsExhausted = false AND a.locked = false AND a.status = 'PENDING'")
    List<ApprovalRound> findPendingRoundsForProcessing();

    // Finding rounds that have passed their deadline (for DLQ)
    @Query("SELECT a FROM ApprovalRound a WHERE a.status = 'PENDING' AND a.deadline < CURRENT_TIMESTAMP")
    List<ApprovalRound> findExpiredRounds();

    List<ApprovalRound> findAllByAgentRequest_Id(String agentRequestId);

    // Finding rounds eligible for callback (not in SQS, not exhausted, not locked, and not PENDING)
    @Query("SELECT a FROM ApprovalRound a WHERE a.inSqs = false AND a.sqsExhausted = false AND a.locked = false AND a.status <> 'PENDING'")
    List<ApprovalRound> findCallbackEligibleRounds();
}
