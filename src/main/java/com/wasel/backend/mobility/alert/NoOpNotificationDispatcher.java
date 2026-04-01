package com.wasel.backend.mobility.alert;

import org.springframework.stereotype.Component;

@Component
public class NoOpNotificationDispatcher implements NotificationDispatcher {

    @Override
    public void dispatch(AlertRecord alertRecord) {
        alertRecord.setDeliveryStatus(AlertDeliveryStatus.DISPATCHED);
    }
}
