package com.example.template.modules.user.controller;

import com.example.template.common.constants.ApiMessages;
import com.example.template.common.response.ApiResponse;
import com.example.template.common.response.PageResponse;
import com.example.template.modules.user.dto.CreateUserRequest;
import com.example.template.modules.user.dto.UpdateUserRequest;
import com.example.template.modules.user.dto.UserResponse;
import com.example.template.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "ADMIN-only user management")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users with pagination and optional keyword filtering")
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(userService.list(keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiMessages.CREATED, userService.create(request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a user")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(ApiMessages.UPDATED, userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(ApiMessages.DELETED, null);
    }
}
