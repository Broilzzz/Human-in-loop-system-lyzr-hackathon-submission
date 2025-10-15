package com.example.lyzr_backend.dto.response;

import java.sql.Timestamp;
import java.util.List;

import com.example.lyzr_backend.entity.AgentRequest;
import com.example.lyzr_backend.entity.ApprovalRound;
import com.example.lyzr_backend.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllAgentRequestWithApprovalRoundsDTO {
    private String agentRequestId;
    private String agentName;
    private List<RoundInfo> approvalRounds;

    public AllAgentRequestWithApprovalRoundsDTO (
            AgentRequest agentRequest, List<ApprovalRound> rounds
    ) {
        this.agentRequestId = agentRequest.getId();
        this.agentName = agentRequest.getAgentName();
        this.approvalRounds = rounds.stream()
                .map(r -> new RoundInfo(r.getId(), r.getRoundNumber(), r.getStatus(), r.getCompletedAt()))
                .toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundInfo{
        private String roundId;
        private Integer roundNumber;
        private RoundStatus status;
        private Timestamp updatedAt;
    }

}
