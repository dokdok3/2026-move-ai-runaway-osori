package com.hackathon.health;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "System")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "API 상태 확인")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
