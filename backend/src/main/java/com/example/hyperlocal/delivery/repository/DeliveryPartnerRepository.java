package com.example.hyperlocal.delivery.repository;

import com.example.hyperlocal.delivery.entity.DeliveryPartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartnerProfile, Long> {
    Optional<DeliveryPartnerProfile> findByUserId(Long userId);
    List<DeliveryPartnerProfile> findByIsOnlineTrue();
}
