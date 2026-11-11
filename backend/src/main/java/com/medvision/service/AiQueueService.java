package com.medvision.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiQueueService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_NAME = "ai_analysis_queue";

    public void enqueueForAnalysis(String studyUID, String instanceUID, String filePath, String patientName) {
        Map<String, String> analysisTask = new HashMap<>();
        analysisTask.put("studyUID", studyUID);
        analysisTask.put("instanceUID", instanceUID);
        analysisTask.put("filePath", filePath);
        analysisTask.put("patientName", patientName);
        analysisTask.put("status", "PENDING");

        redisTemplate.opsForList().leftPush(QUEUE_NAME, analysisTask);
        System.out.println("Tarefa de análise enviada para a fila do Redis: StudyUID=" + studyUID);
    }
}
