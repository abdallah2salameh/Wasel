package com.wasel.backend.mobility.checkpoint;

import com.wasel.backend.common.PageResponse;
import com.wasel.backend.common.ResourceNotFoundException;
import com.wasel.backend.security.UserAccount;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class CheckpointService {

    private final CheckpointRepository checkpointRepository;
    private final CheckpointStatusHistoryRepository checkpointStatusHistoryRepository;

    public CheckpointService(
            CheckpointRepository checkpointRepository,
            CheckpointStatusHistoryRepository checkpointStatusHistoryRepository
    ) {
        this.checkpointRepository = checkpointRepository;
        this.checkpointStatusHistoryRepository = checkpointStatusHistoryRepository;
    }

    @Transactional
    @CacheEvict(value = "checkpoints", allEntries = true)
    public CheckpointDtos.CheckpointResponse create(CheckpointDtos.CreateCheckpointRequest request, UserAccount actor) {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setName(request.name());
        checkpoint.setGovernorate(request.governorate());
        checkpoint.setLatitude(request.latitude());
        checkpoint.setLongitude(request.longitude());
        checkpoint.setCurrentStatus(request.currentStatus());
        checkpoint.setNotes(request.notes());
        checkpointRepository.save(checkpoint);
        createHistory(checkpoint, request.currentStatus(), request.notes(), actor);
        return toResponse(checkpoint);
    }

    @Cacheable(value = "checkpoints", key = "#governorate + '-' + #status + '-' + #page + '-' + #size + '-' + #sortBy")
    public PageResponse<CheckpointDtos.CheckpointResponse> list(
            String governorate,
            CheckpointStatus status,
            int page,
            int size,
            String sortBy
    ) {
        Specification<Checkpoint> specification = Specification.where(null);
        if (StringUtils.hasText(governorate)) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("governorate")), governorate.toLowerCase()));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("currentStatus"), status));
        }

        Page<CheckpointDtos.CheckpointResponse> response = checkpointRepository.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, normalizeSort(sortBy, "updatedAt"))))
                .map(this::toResponse);
        return PageResponse.from(response);
    }

    public CheckpointDtos.CheckpointDetailsResponse details(UUID id) {
        Checkpoint checkpoint = checkpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
        List<CheckpointDtos.CheckpointHistoryResponse> history = checkpointStatusHistoryRepository.findByCheckpointIdOrderByRecordedAtDesc(id)
                .stream()
                .map(item -> new CheckpointDtos.CheckpointHistoryResponse(
                        item.getId(),
                        item.getStatus(),
                        item.getNotes(),
                        item.getChangedBy() != null ? item.getChangedBy().getEmail() : "system",
                        item.getRecordedAt()
                ))
                .toList();
        return new CheckpointDtos.CheckpointDetailsResponse(toResponse(checkpoint), history);
    }

    @Transactional
    @CacheEvict(value = "checkpoints", allEntries = true)
    public CheckpointDtos.CheckpointResponse updateStatus(UUID id, CheckpointDtos.UpdateCheckpointStatusRequest request, UserAccount actor) {
        Checkpoint checkpoint = checkpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found"));
        checkpoint.setCurrentStatus(request.status());
        checkpoint.setNotes(request.notes());
        createHistory(checkpoint, request.status(), request.notes(), actor);
        return toResponse(checkpoint);
    }

    private void createHistory(Checkpoint checkpoint, CheckpointStatus status, String notes, UserAccount actor) {
        CheckpointStatusHistory history = new CheckpointStatusHistory();
        history.setCheckpoint(checkpoint);
        history.setStatus(status);
        history.setNotes(notes);
        history.setChangedBy(actor);
        checkpointStatusHistoryRepository.save(history);
    }

    private String normalizeSort(String sortBy, String fallback) {
        String candidate = sortBy == null ? fallback : sortBy;
        return switch (candidate) {
            case "name", "createdAt", "updatedAt", "governorate" -> candidate;
            default -> fallback;
        };
    }

    public CheckpointDtos.CheckpointResponse toResponse(Checkpoint checkpoint) {
        return new CheckpointDtos.CheckpointResponse(
                checkpoint.getId(),
                checkpoint.getName(),
                checkpoint.getGovernorate(),
                checkpoint.getLatitude(),
                checkpoint.getLongitude(),
                checkpoint.getCurrentStatus(),
                checkpoint.getNotes(),
                checkpoint.getCreatedAt(),
                checkpoint.getUpdatedAt()
        );
    }
}
