package com.chaerok.backend.notification.device.repository;

import com.chaerok.backend.notification.device.entity.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository
        extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByFcmRegistrationToken(
            String fcmRegistrationToken
    );

    List<PushDevice> findAllByUserId(Long userId);

    void deleteByUserIdAndFcmRegistrationToken(
            Long userId,
            String fcmRegistrationToken
    );
}