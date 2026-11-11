package com.medvision.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${auth.api-key}")
    private String apiKey;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void validateApiKey() {
        if (apiKey.contains("change-me")) {
            throw new IllegalStateException(
                    "API key contains default placeholder value. Set AUTH_API_KEY environment variable.");
        }
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody Map<String, String> request) {
        String key = request.get("apiKey");
        String clientId = request.get("clientId");

        if (key != null && timingSafeEquals(apiKey, key) && clientId != null && !clientId.isBlank()) {
            String token = jwtUtil.generateToken(clientId);
            return ResponseEntity.ok(Map.of("token", token, "type", "Bearer"));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid API key"));
    }

    private boolean timingSafeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
