package com.example.hyperlocal.vendor.repository;

import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByOwnerUserId(Long ownerUserId);
    List<Vendor> findByStatus(VendorStatus status);
    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);
}
