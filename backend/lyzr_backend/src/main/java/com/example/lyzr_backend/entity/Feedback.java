package com.example.lyzr_backend.entity;

import java.sql.Timestamp;

import com.example.lyzr_backend.enums.RoundStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "approvalRoundId")
    private ApprovalRound approvalRound;

    @ManyToOne
    @JoinColumn(name = "employeeId")
    private Employee employee;

    private String comment;

    private Timestamp timestamp;
}
