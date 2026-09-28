package com.example.hyperlocal.admin;

import com.example.hyperlocal.admin.dto.AdminDashboardMetricsDto;
import com.example.hyperlocal.admin.service.AdminService;
import com.example.hyperlocal.user.dto.UserDto;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.entity.UserStatus;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class AdminGovernanceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testAdminMetricsAndGovernanceFlows() {
        // 1. Metrics dashboard check
        AdminDashboardMetricsDto metrics = adminService.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getTotalVendors() >= 3);
        assertTrue(metrics.getTotalCustomers() >= 1);
        assertTrue(metrics.getTotalDeliveryPartners() >= 1);
        assertNotNull(metrics.getTotalGmv());

        // 2. Vendor Approval & Suspension Flow
        User pendingOwner = new User("Pending Store Owner", "pending@hyperlocal.com", "+919000000099", "pass", Role.VENDOR);
        userRepository.save(pendingOwner);

        Vendor newVendor = new Vendor(
                pendingOwner.getId(),
                "New Pending Bakery",
                "Artisan sourdough and croissants",
                "+919845000099",
                "55, 12th Main, Koramangala",
                12.9350,
                77.6240,
                5.0
        );
        newVendor.setStatus(VendorStatus.PENDING_APPROVAL);
        Vendor savedPending = vendorRepository.save(newVendor);

        // Fetch pending vendors
        Page<VendorDto> pendingList = adminService.getVendors(VendorStatus.PENDING_APPROVAL, 0, 10);
        assertTrue(pendingList.getContent().stream().anyMatch(v -> v.getId().equals(savedPending.getId())));

        // Approve vendor
        VendorDto approved = adminService.approveVendor(savedPending.getId());
        assertEquals(VendorStatus.ACTIVE, approved.getStatus());

        // Suspend vendor
        VendorDto suspended = adminService.suspendVendor(savedPending.getId());
        assertEquals(VendorStatus.SUSPENDED, suspended.getStatus());

        // 3. User Search & Status Management
        Page<UserDto> customers = adminService.getUsers(Role.CUSTOMER, null, 0, 10);
        assertFalse(customers.isEmpty());
        UserDto targetUser = customers.getContent().get(0);

        // Suspend user
        UserDto suspendedUser = adminService.updateUserStatus(targetUser.getId(), UserStatus.SUSPENDED);
        assertEquals(UserStatus.SUSPENDED, suspendedUser.getStatus());

        // Reactivate user
        UserDto reactivatedUser = adminService.updateUserStatus(targetUser.getId(), UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, reactivatedUser.getStatus());
    }
}
