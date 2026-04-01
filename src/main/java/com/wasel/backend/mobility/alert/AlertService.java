package com.wasel.backend.mobility.alert;

import com.wasel.backend.common.ResourceNotFoundException;
import com.wasel.backend.mobility.GeoUtils;
import com.wasel.backend.mobility.incident.Incident;
import com.wasel.backend.security.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertSubscriptionRepository alertSubscriptionRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final NotificationDispatcher notificationDispatcher;

    public AlertService(
            AlertSubscriptionRepository alertSubscriptionRepository,
            AlertRecordRepository alertRecordRepository,
            NotificationDispatcher notificationDispatcher
    ) {
        this.alertSubscriptionRepository = alertSubscriptionRepository;
        this.alertRecordRepository = alertRecordRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public AlertDtos.SubscriptionResponse createSubscription(AlertDtos.CreateSubscriptionRequest request, UserAccount user) {
        AlertSubscription subscription = new AlertSubscription();
        subscription.setUser(user);
        subscription.setAreaName(request.areaName());
        subscription.setMinLatitude(Math.min(request.minLatitude(), request.maxLatitude()));
        subscription.setMaxLatitude(Math.max(request.minLatitude(), request.maxLatitude()));
        subscription.setMinLongitude(Math.min(request.minLongitude(), request.maxLongitude()));
        subscription.setMaxLongitude(Math.max(request.minLongitude(), request.maxLongitude()));
        subscription.setIncidentCategory(request.incidentCategory());
        subscription.setActive(true);
        alertSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(subscription);
    }

    public List<AlertDtos.SubscriptionResponse> mySubscriptions(UserAccount user) {
        return alertSubscriptionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toSubscriptionResponse)
                .toList();
    }

    public List<AlertDtos.AlertRecordResponse> myAlerts(UserAccount user) {
        return alertRecordRepository.findByUser(user)
                .stream()
                .map(record -> new AlertDtos.AlertRecordResponse(
                        record.getId(),
                        record.getIncident().getId(),
                        record.getIncident().getTitle(),
                        record.getDeliveryStatus(),
                        record.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void deactivate(UUID id, UserAccount user) {
        AlertSubscription subscription = alertSubscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert subscription not found"));
        if (!subscription.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Cannot modify another user's subscription");
        }
        subscription.setActive(false);
    }

    @Transactional
    public void createAlertRecordsForIncident(Incident incident) {
        List<AlertSubscription> subscriptions = alertSubscriptionRepository.findByActiveTrue().stream()
                .filter(subscription -> subscription.getIncidentCategory() == null
                        || subscription.getIncidentCategory() == incident.getCategory())
                .filter(subscription -> GeoUtils.insideBounds(
                        incident.getLatitude(),
                        incident.getLongitude(),
                        subscription.getMinLatitude(),
                        subscription.getMaxLatitude(),
                        subscription.getMinLongitude(),
                        subscription.getMaxLongitude()))
                .toList();

        for (AlertSubscription subscription : subscriptions) {
            AlertRecord alertRecord = new AlertRecord();
            alertRecord.setSubscription(subscription);
            alertRecord.setIncident(incident);
            alertRecord.setDeliveryStatus(AlertDeliveryStatus.PENDING);
            alertRecordRepository.save(alertRecord);
            notificationDispatcher.dispatch(alertRecord);
        }
    }

    private AlertDtos.SubscriptionResponse toSubscriptionResponse(AlertSubscription subscription) {
        return new AlertDtos.SubscriptionResponse(
                subscription.getId(),
                subscription.getAreaName(),
                subscription.getMinLatitude(),
                subscription.getMaxLatitude(),
                subscription.getMinLongitude(),
                subscription.getMaxLongitude(),
                subscription.getIncidentCategory(),
                subscription.isActive(),
                subscription.getCreatedAt()
        );
    }
}
