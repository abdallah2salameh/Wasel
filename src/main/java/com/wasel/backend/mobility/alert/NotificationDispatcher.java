package com.wasel.backend.mobility.alert;

public interface NotificationDispatcher {
    void dispatch(AlertRecord alertRecord);
}
