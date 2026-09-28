package com.example.hyperlocal.vendor;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.service.AddressService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.location.service.LocationService;
import com.example.hyperlocal.vendor.dto.StoreStatusUpdateRequest;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.dto.VendorRegistrationRequest;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class LocationAndVendorDiscoveryTest {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AddressService addressService;

    @Test
    void testVendorDiscoveryAndHaversineRadius() {
        // Customer at Koramangala (12.9352, 77.6245)
        Page<VendorDto> nearby = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 10);
        assertNotNull(nearby);
        assertFalse(nearby.getContent().isEmpty(), "Should discover nearby vendors seeded on startup");

        // The nearest vendor should be Fresh Mart (< 1.5 km away)
        VendorDto first = nearby.getContent().get(0);
        assertTrue(first.getDistanceKm() <= first.getDeliveryRadiusKm());
        assertTrue(first.getDistanceKm() < 2.0, "Koramangala store should be very close to customer in Koramangala");

        // Customer in faraway location (e.g., Delhi: 28.6139, 77.2090)
        Page<VendorDto> delhiNearby = vendorService.getNearbyVendors(28.6139, 77.2090, "distance", 0, 10);
        assertTrue(delhiNearby.getContent().isEmpty(), "No Bangalore stores should deliver to Delhi");
    }

    @Test
    void testManualOpenOverride() {
        List<VendorDto> vendors = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 10).getContent();
        VendorDto target = vendors.get(0);

        // Toggle store to closed manually
        StoreStatusUpdateRequest closeReq = new StoreStatusUpdateRequest();
        closeReq.setManualOpenOverride(false);
        VendorDto updated = vendorService.updateStoreStatus(target.getOwnerUserId(), closeReq);
        assertFalse(updated.getIsOpen(), "Store should now be closed due to manual override");

        // Toggle back to open
        StoreStatusUpdateRequest openReq = new StoreStatusUpdateRequest();
        openReq.setManualOpenOverride(true);
        VendorDto reopened = vendorService.updateStoreStatus(target.getOwnerUserId(), openReq);
        assertTrue(reopened.getIsOpen(), "Store should now be open due to manual override");
    }

    @Test
    void testServiceabilityCheck() {
        // Koramangala coords (covered by seed vendors)
        Map<String, Object> serviceable = locationService.checkServiceability(12.9352, 77.6245);
        assertTrue((Boolean) serviceable.get("serviceable"));
        assertTrue((Long) serviceable.get("vendorsNearby") > 0);

        // Remote coords (not covered)
        Map<String, Object> notServiceable = locationService.checkServiceability(28.6139, 77.2090);
        assertFalse((Boolean) notServiceable.get("serviceable"));
        assertEquals(0L, notServiceable.get("vendorsNearby"));
    }

    @Test
    void testAddressManagementAndSingleDefaultInvariant() {
        Long testUserId = 9999L;

        AddressRequest addr1 = new AddressRequest();
        addr1.setLabel("Home");
        addr1.setHouse("No 10");
        addr1.setStreet("MG Road");
        addr1.setLocality("Central");
        addr1.setCity("Bengaluru");
        addr1.setState("Karnataka");
        addr1.setPostalCode("560001");
        addr1.setLatitude(12.9756);
        addr1.setLongitude(77.6066);
        addr1.setIsDefault(true);

        AddressDto saved1 = addressService.createAddress(testUserId, addr1);
        assertTrue(saved1.getIsDefault());

        // Add second address with isDefault = true
        AddressRequest addr2 = new AddressRequest();
        addr2.setLabel("Work");
        addr2.setHouse("Tech Park");
        addr2.setStreet("Outer Ring Road");
        addr2.setLocality("Bellandur");
        addr2.setCity("Bengaluru");
        addr2.setState("Karnataka");
        addr2.setPostalCode("560103");
        addr2.setLatitude(12.9260);
        addr2.setLongitude(77.6762);
        addr2.setIsDefault(true);

        AddressDto saved2 = addressService.createAddress(testUserId, addr2);
        assertTrue(saved2.getIsDefault());

        // Verify that addr1 is now NOT default (single default address invariant)
        AddressDto refetched1 = addressService.getAddressById(saved1.getId(), testUserId);
        assertFalse(refetched1.getIsDefault(), "First address must no longer be default when second address was marked default");

        // Verify ownership check: another user cannot fetch or delete this address
        Long otherUserId = 8888L;
        assertThrows(ApiException.class, () -> addressService.getAddressById(saved1.getId(), otherUserId));
        assertThrows(ApiException.class, () -> addressService.deleteAddress(saved1.getId(), otherUserId));
    }
}
