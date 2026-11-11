package com.medvision.controller;

import com.medvision.model.AnalysisResult;
import com.medvision.repository.AnalysisResultRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/results")
public class AnalysisController {

    @Autowired
    private AnalysisResultRepository repository;

    @GetMapping
    public List<AnalysisResult> getAllResults() {
        return repository.findAllByOrderByPriorityLevelDescCreatedAtDesc();
    }

    @PostMapping
    public AnalysisResult saveResult(@Valid @RequestBody AnalysisResult result) {
        // Fallback for fields if missing in JSON but provided in flat result
        if (result.getStatus() == null) result.setStatus("COMPLETED");

        // Lógica de priorização automática
        if (result.isAnomalyDetected() && result.getConfidence() > 0.9) {
            result.setPriority("CRITICAL");
            result.setPriorityLevel(3);
        } else if (result.isAnomalyDetected()) {
            result.setPriority("MEDIUM");
            result.setPriorityLevel(2);
        } else {
            result.setPriority("LOW");
            result.setPriorityLevel(1);
        }
        return repository.save(result);
    }
}
