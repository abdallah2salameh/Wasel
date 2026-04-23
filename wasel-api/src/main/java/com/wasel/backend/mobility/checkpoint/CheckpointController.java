package com.wasel.backend.mobility.checkpoint;

import com.wasel.backend.common.PageResponse;
import com.wasel.backend.security.UserAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkpoints")
public class CheckpointController {

    private final CheckpointService checkpointService;

    public CheckpointController(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckpointDtos.CheckpointResponse create(
            @Valid @RequestBody CheckpointDtos.CreateCheckpointRequest request,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return checkpointService.create(request, actor);
    }

    @GetMapping
    public PageResponse<CheckpointDtos.CheckpointResponse> list(
            @RequestParam(required = false) String governorate,
            @RequestParam(required = false) CheckpointStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy
    ) {
        return checkpointService.list(governorate, status, page, size, sortBy);
    }

    @GetMapping("/{id}")
    public CheckpointDtos.CheckpointDetailsResponse details(@PathVariable UUID id) {
        return checkpointService.details(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public CheckpointDtos.CheckpointResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CheckpointDtos.UpdateCheckpointStatusRequest request,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return checkpointService.updateStatus(id, request, actor);
    }
}
