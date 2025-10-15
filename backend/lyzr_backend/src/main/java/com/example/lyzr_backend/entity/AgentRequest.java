package com.example.lyzr_backend.entity;

import java.sql.Timestamp;

import com.example.lyzr_backend.enums.RequestStatus;
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
public class AgentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String callbackUrl;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)  // <-- Join column name
    private Employee employee;

    // last rounds status
    @Enumerated(EnumType.STRING)
    private RequestStatus currentStatus;

    @Column(nullable = false)
    private Timestamp createdAt;

    private Timestamp updatedAt;

}
