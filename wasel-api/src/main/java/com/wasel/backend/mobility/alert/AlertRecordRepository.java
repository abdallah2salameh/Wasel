package com.wasel.backend.mobility.alert;

import com.wasel.backend.security.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, UUID> {

    @Query("""
            select ar
            from AlertRecord ar
            join ar.subscription s
            where s.user = :user
            order by ar.createdAt desc
            """)
    List<AlertRecord> findByUser(UserAccount user);
}
