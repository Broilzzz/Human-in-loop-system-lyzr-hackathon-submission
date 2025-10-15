package com.example.lyzr_backend.entity;

import java.sql.Timestamp;
import java.util.List;

import com.example.lyzr_backend.enums.RoundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRound {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "agentRequestId")
    private AgentRequest agentRequest;

    @Column(nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    private RoundStatus status;

    private Timestamp completedAt;
    private Timestamp startedAt;

    private Boolean inSqs;
    private Boolean locked;
    private Boolean sqsExhausted;

    // subject of the problem
    @Column(columnDefinition = "TEXT")
    private String subject;

    // contains all the context of the problem
    @Column(columnDefinition = "TEXT")
    private String context;

    private Timestamp lastSqsSentAt;
    private Timestamp deadline;

    // Exponential backoff fields
    @Column(nullable = false)
    private Integer backoffMultiplier = 1;

}
