package com.example.lyzr_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lyzr_backend.entity.AgentRequest;

@Repository
public interface AgentRequestRepository extends JpaRepository<AgentRequest, String> {
    List<AgentRequest> findByEmployee_Id(String employeeId);
}
