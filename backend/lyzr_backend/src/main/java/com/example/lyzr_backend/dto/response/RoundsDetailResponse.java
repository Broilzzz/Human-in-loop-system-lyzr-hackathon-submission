package com.example.lyzr_backend.dto.response;

import java.util.List;

import com.example.lyzr_backend.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundsDetailResponse {
    private RoundStatus status;
    private String subject;
    private String agentName;
    private String context;
    private List<String> S3Links;
}
