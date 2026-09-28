package com.example.hyperlocal.admin.service;

import com.example.hyperlocal.admin.dto.AdminDashboardMetricsDto;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.delivery.repository.DeliveryPartnerRepository;
import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.repository.OrderRepository;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.entity.UserStatus;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public AdminService(
            VendorRepository vendorRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            DeliveryPartnerRepository deliveryPartnerRepository) {
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardMetricsDto getMetrics() {
        List<Vendor> allVendors = vendorRepository.findAll();
        long activeVendors = allVendors.stream().filter(v -> v.getStatus() == VendorStatus.ACTIVE).count();
        long pendingVendors = allVendors.stream().filter(v -> v.getStatus() == VendorStatus.PENDING_APPROVAL).count();

        List<Order> allOrders = orderRepository.findAll();
        long completed = allOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED).count();
        long active = allOrders.stream().filter(o ->
                o.getOrderStatus() == OrderStatus.VENDOR_PENDING ||
                o.getOrderStatus() == OrderStatus.ACCEPTED ||
                o.getOrderStatus() == OrderStatus.PREPARING ||
                o.getOrderStatus() == OrderStatus.READY ||
                o.getOrderStatus() == OrderStatus.DELIVERY_ASSIGNED ||
                o.getOrderStatus() == OrderStatus.PICKED_UP ||
                o.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY
        ).count();

        BigDecimal gmv = allOrders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED &&
                             o.getOrderStatus() != OrderStatus.REJECTED &&
                             o.getOrderStatus() != OrderStatus.PAYMENT_FAILED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AdminDashboardMetricsDto metrics = new AdminDashboardMetricsDto();
        metrics.setTotalVendors((long) allVendors.size());
        metrics.setActiveVendors(activeVendors);
        metrics.setPendingVendors(pendingVendors);
        metrics.setTotalCustomers(userRepository.countByRole(Role.CUSTOMER));
        metrics.setTotalDeliveryPartners(userRepository.countByRole(Role.DELIVERY_PARTNER));
        metrics.setTotalOrders((long) allOrders.size());
        metrics.setTotalGmv(gmv);
        metrics.setCompletedOrders(completed);
        metrics.setActiveOrders(active);

        return metrics;
    }

    @Transactional(readOnly = true)
    public Page<VendorDto> getVendors(VendorStatus status, int page, int size) {
        Page<Vendor> vendorPage;
        if (status != null) {
            vendorPage = vendorRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            vendorPage = vendorRepository.findAll(PageRequest.of(page, size));
        }

        List<VendorDto> dtos = vendorPage.getContent().stream()
                .map(v -> VendorDto.fromEntity(v, 0.0, v.getStatus() == VendorStatus.ACTIVE))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, PageRequest.of(page, size), vendorPage.getTotalElements());
    }

    @Transactional
    public VendorDto approveVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        vendor.setStatus(VendorStatus.ACTIVE);
        Vendor saved = vendorRepository.save(vendor);
        return VendorDto.fromEntity(saved, 0.0, true);
    }

    @Transactional
    public VendorDto suspendVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        vendor.setStatus(VendorStatus.SUSPENDED);
        Vendor saved = vendorRepository.save(vendor);
        return VendorDto.fromEntity(saved, 0.0, false);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Role role, String query, int page, int size) {
        String q = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        Page<User> users = userRepository.searchUsers(role, q, PageRequest.of(page, size));
        List<UserDto> dtos = users.getContent().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(page, size), users.getTotalElements());
    }

    @Transactional
    public UserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        user.setStatus(status);
        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }
}
