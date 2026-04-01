package com.wasel.backend.mobility.alert;

import com.wasel.backend.security.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, UUID> {
    List<AlertSubscription> findByUserOrderByCreatedAtDesc(UserAccount user);

    List<AlertSubscription> findByActiveTrue();
}
