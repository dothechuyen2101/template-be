package com.example.template.modules.publicapi.controller;

import com.example.template.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public", description = "Public endpoints")
@SecurityRequirements
public class PublicController {

    @GetMapping("/ping")
    @Operation(summary = "Public liveness example")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(Map.of("status", "pong"));
    }
}
