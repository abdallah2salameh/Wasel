package com.wasel.backend.mobility.report;

import com.wasel.backend.common.PageResponse;
import com.wasel.backend.security.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDtos.ReportResponse submit(
            @Valid @RequestBody ReportDtos.CreateReportRequest request,
            @AuthenticationPrincipal UserAccount user,
            @RequestHeader(value = "X-Client-Fingerprint", required = false) String fingerprint,
            HttpServletRequest servletRequest
    ) {
        String resolvedFingerprint = fingerprint != null ? fingerprint : servletRequest.getRemoteAddr();
        ReportDtos.ReportResponse response = reportService.submit(request, user, resolvedFingerprint);
        String userEmail = user != null ? user.getEmail() : "anonymous";
        logger.info("Report submitted successfully by user: {}, category: {}", userEmail, request.category());
        return response;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public PageResponse<ReportDtos.ReportResponse> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.list(status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public ReportDtos.ReportDetailsResponse details(@PathVariable UUID id) {
        return reportService.details(id);
    }

    @PostMapping("/{id}/vote")
    public ReportDtos.ReportResponse vote(
            @PathVariable UUID id,
            @Valid @RequestBody ReportDtos.VoteRequest request,
            @AuthenticationPrincipal UserAccount user
    ) {
        return reportService.vote(id, request, user);
    }

    @PostMapping("/{id}/moderate")
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    public ReportDtos.ReportResponse moderate(
            @PathVariable UUID id,
            @Valid @RequestBody ReportDtos.ModerateReportRequest request,
            @AuthenticationPrincipal UserAccount actor
    ) {
        return reportService.moderate(id, request, actor);
    }
}
