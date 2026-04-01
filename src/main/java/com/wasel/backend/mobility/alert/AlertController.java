package com.wasel.backend.mobility.alert;

import com.wasel.backend.security.UserAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertDtos.SubscriptionResponse subscribe(
            @Valid @RequestBody AlertDtos.CreateSubscriptionRequest request,
            @AuthenticationPrincipal UserAccount user
    ) {
        return alertService.createSubscription(request, user);
    }

    @GetMapping("/subscriptions")
    public List<AlertDtos.SubscriptionResponse> mySubscriptions(@AuthenticationPrincipal UserAccount user) {
        return alertService.mySubscriptions(user);
    }

    @DeleteMapping("/subscriptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal UserAccount user) {
        alertService.deactivate(id, user);
    }

    @GetMapping("/records")
    public List<AlertDtos.AlertRecordResponse> myAlerts(@AuthenticationPrincipal UserAccount user) {
        return alertService.myAlerts(user);
    }
}
