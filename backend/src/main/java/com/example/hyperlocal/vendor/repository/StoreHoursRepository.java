package com.example.hyperlocal.vendor.repository;

import com.example.hyperlocal.vendor.entity.StoreHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreHoursRepository extends JpaRepository<StoreHours, Long> {
    List<StoreHours> findByVendorId(Long vendorId);
    Optional<StoreHours> findByVendorIdAndDayOfWeek(Long vendorId, Integer dayOfWeek);
}
