package com.example.hyperlocal.vendor.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.common.util.HaversineUtil;
import com.example.hyperlocal.vendor.dto.StoreStatusUpdateRequest;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.dto.VendorRegistrationRequest;
import com.example.hyperlocal.vendor.entity.StoreHours;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.StoreHoursRepository;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final StoreHoursRepository storeHoursRepository;

    public VendorService(VendorRepository vendorRepository, StoreHoursRepository storeHoursRepository) {
        this.vendorRepository = vendorRepository;
        this.storeHoursRepository = storeHoursRepository;
    }

    @Transactional(readOnly = true)
    public Page<VendorDto> getNearbyVendors(Double customerLat, Double customerLng, String sort, int page, int size) {
        List<Vendor> activeVendors = vendorRepository.findByStatus(VendorStatus.ACTIVE);

        List<VendorDto> serviceable = activeVendors.stream()
                .map(v -> {
                    double dist = customerLat != null && customerLng != null
                            ? HaversineUtil.distance(customerLat, customerLng, v.getLatitude(), v.getLongitude())
                            : 0.0;
                    boolean isOpen = calculateIsOpen(v);
                    return VendorDto.fromEntity(v, dist, isOpen);
                })
                .filter(dto -> customerLat == null || customerLng == null || dto.getDistanceKm() <= dto.getDeliveryRadiusKm())
                .collect(Collectors.toList());

        // Sorting
        if ("open".equalsIgnoreCase(sort)) {
            serviceable.sort(Comparator.comparing((VendorDto v) -> !Boolean.TRUE.equals(v.getIsOpen()))
                    .thenComparing(VendorDto::getDistanceKm));
        } else {
            // Default: distance ascending
            serviceable.sort(Comparator.comparing(VendorDto::getDistanceKm));
        }

        // In-memory pagination for distance-ranked candidates
        int start = Math.min(page * size, serviceable.size());
        int end = Math.min(start + size, serviceable.size());
        List<VendorDto> pageContent = serviceable.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), serviceable.size());
    }

    @Transactional(readOnly = true)
    public VendorDto getVendorById(Long vendorId, Double customerLat, Double customerLng) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        double dist = customerLat != null && customerLng != null
                ? HaversineUtil.distance(customerLat, customerLng, vendor.getLatitude(), vendor.getLongitude())
                : 0.0;
        boolean isOpen = calculateIsOpen(vendor);
        return VendorDto.fromEntity(vendor, dist, isOpen);
    }

    @Transactional(readOnly = true)
    public List<VendorDto> searchVendors(String query, Double customerLat, Double customerLng) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = query.trim().toLowerCase();

        return vendorRepository.findByStatus(VendorStatus.ACTIVE).stream()
                .filter(v -> v.getStoreName().toLowerCase().contains(q) ||
                             (v.getDescription() != null && v.getDescription().toLowerCase().contains(q)) ||
                             v.getAddress().toLowerCase().contains(q))
                .map(v -> {
                    double dist = customerLat != null && customerLng != null
                            ? HaversineUtil.distance(customerLat, customerLng, v.getLatitude(), v.getLongitude())
                            : 0.0;
                    return VendorDto.fromEntity(v, dist, calculateIsOpen(v));
                })
                .sorted(Comparator.comparing(VendorDto::getDistanceKm))
                .collect(Collectors.toList());
    }

    @Transactional
    public VendorDto registerStore(Long ownerUserId, VendorRegistrationRequest req) {
        vendorRepository.findByOwnerUserId(ownerUserId).ifPresent(v -> {
            throw new ApiException("VENDOR_ALREADY_EXISTS", "A store is already registered for this account", HttpStatus.CONFLICT);
        });

        Vendor vendor = new Vendor(
                ownerUserId,
                req.getStoreName().trim(),
                req.getDescription() != null ? req.getDescription().trim() : null,
                req.getPhone().trim(),
                req.getAddress().trim(),
                req.getLatitude(),
                req.getLongitude(),
                req.getDeliveryRadiusKm()
        );
        if (req.getImageUrl() != null) {
            vendor.setImageUrl(req.getImageUrl().trim());
        }

        Vendor saved = vendorRepository.save(vendor);

        // Seed default store hours (Mon-Sun 08:00 - 22:00)
        for (int day = 1; day <= 7; day++) {
            StoreHours hours = new StoreHours(saved.getId(), day, LocalTime.of(8, 0), LocalTime.of(22, 0), true);
            storeHoursRepository.save(hours);
        }

        return VendorDto.fromEntity(saved, 0.0, true);
    }

    @Transactional(readOnly = true)
    public VendorDto getStoreByOwner(Long ownerUserId) {
        Vendor vendor = vendorRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "No store registered for this user", HttpStatus.NOT_FOUND));
        return VendorDto.fromEntity(vendor, 0.0, calculateIsOpen(vendor));
    }

    @Transactional
    public VendorDto updateStoreStatus(Long ownerUserId, StoreStatusUpdateRequest req) {
        Vendor vendor = vendorRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "No store registered for this user", HttpStatus.NOT_FOUND));

        vendor.setManualOpenOverride(req.getManualOpenOverride());
        Vendor updated = vendorRepository.save(vendor);
        return VendorDto.fromEntity(updated, 0.0, calculateIsOpen(updated));
    }

    public boolean calculateIsOpen(Vendor vendor) {
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            return false;
        }
        if (vendor.getManualOpenOverride() != null) {
            return vendor.getManualOpenOverride();
        }

        // Calculate schedule-based open status
        int today = LocalDate.now().getDayOfWeek().getValue();
        LocalTime now = LocalTime.now();

        return storeHoursRepository.findByVendorIdAndDayOfWeek(vendor.getId(), today)
                .map(hours -> hours.isOpenNow(now))
                .orElse(true); // Default open if no specific hours configured
    }
}
