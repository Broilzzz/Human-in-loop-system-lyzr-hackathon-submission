package com.example.lyzr_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lyzr_backend.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmail(String email);
}
