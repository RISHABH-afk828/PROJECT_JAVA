package com.example.hyperlocal.delivery;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.service.AddressService;
import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.service.CartService;
import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.service.CheckoutService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.delivery.dto.DeliveryLocationUpdateRequest;
import com.example.hyperlocal.delivery.dto.DeliveryProfileDto;
import com.example.hyperlocal.delivery.dto.DeliveryStatusUpdateRequest;
import com.example.hyperlocal.delivery.service.DeliveryService;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.payment.dto.PaymentOrderResponse;
import com.example.hyperlocal.payment.dto.PaymentVerifyRequest;
import com.example.hyperlocal.payment.service.PaymentService;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.service.ProductService;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.service.VendorOrderService;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class DeliveryWorkflowTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorOrderService vendorOrderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDeliveryPartnerFullLifecycle() {
        // Users
        User customer = userRepository.findByEmail("customer@hyperlocal.com").orElseThrow();
        User deliveryPartner = userRepository.findByEmail("delivery@hyperlocal.com").orElseThrow();
        Long deliveryId = deliveryPartner.getId();

        // 1. Profile initialization & Online toggle
        DeliveryProfileDto profile = deliveryService.getOrCreateProfile(deliveryId);
        assertNotNull(profile);
        assertFalse(profile.getIsOnline());

        // Test offline check
        assertThrows(ApiException.class, () -> deliveryService.getAvailableOrders(deliveryId),
                "Offline delivery partner should not be able to fetch available orders");

        // Go Online
        DeliveryStatusUpdateRequest statusReq = new DeliveryStatusUpdateRequest();
        statusReq.setIsOnline(true);
        DeliveryProfileDto onlineProfile = deliveryService.updateOnlineStatus(deliveryId, statusReq);
        assertTrue(onlineProfile.getIsOnline());

        // 2. Update Location
        DeliveryLocationUpdateRequest locReq = new DeliveryLocationUpdateRequest();
        locReq.setLatitude(12.9360);
        locReq.setLongitude(77.6250);
        DeliveryProfileDto updatedLocProfile = deliveryService.updateLocation(deliveryId, locReq);
        assertEquals(12.9360, updatedLocProfile.getCurrentLatitude());
        assertEquals(77.6250, updatedLocProfile.getCurrentLongitude());

        // 3. Create an order and progress through Vendor fulfillment to READY
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

        AddressRequest addrReq = new AddressRequest();
        addrReq.setLabel("Home");
        addrReq.setHouse("202");
        addrReq.setStreet("80ft Rd");
        addrReq.setLocality("Koramangala");
        addrReq.setCity("Bengaluru");
        addrReq.setState("Karnataka");
        addrReq.setPostalCode("560034");
        addrReq.setLatitude(12.9352);
        addrReq.setLongitude(77.6245);
        AddressDto address = addressService.createAddress(customer.getId(), addrReq);

        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(1);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        OrderDto order = checkoutService.createOrder(customer.getId(), orderReq);

        PaymentOrderResponse pOrder = paymentService.initiatePayment(customer.getId(), order.getId());
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest();
        verifyReq.setOrderId(order.getId());
        verifyReq.setRazorpayOrderId(pOrder.getProviderOrderId());
        verifyReq.setRazorpayPaymentId("pay_del_001");
        verifyReq.setRazorpaySignature("mock_valid_signature");
        paymentService.verifyPayment(customer.getId(), verifyReq);

        vendorOrderService.acceptOrder(vendor.getOwnerUserId(), order.getId());
        vendorOrderService.startPreparingOrder(vendor.getOwnerUserId(), order.getId());
        OrderDto readyOrder = vendorOrderService.markOrderReady(vendor.getOwnerUserId(), order.getId());
        assertEquals(OrderStatus.READY, readyOrder.getOrderStatus());

        // 4. Delivery partner sees the order in available tasks
        List<OrderDto> availableOrders = deliveryService.getAvailableOrders(deliveryId);
        assertFalse(availableOrders.isEmpty());
        assertTrue(availableOrders.stream().anyMatch(o -> o.getId().equals(order.getId())));

        // 5. Accept / Claim Order
        OrderDto assignedOrder = deliveryService.acceptOrder(deliveryId, order.getId());
        assertEquals(OrderStatus.DELIVERY_ASSIGNED, assignedOrder.getOrderStatus());
        assertEquals(deliveryId, assignedOrder.getDeliveryPartnerId());

        // 6. Confirm Pickup from Store
        OrderDto pickedUpOrder = deliveryService.confirmPickup(deliveryId, order.getId());
        assertEquals(OrderStatus.PICKED_UP, pickedUpOrder.getOrderStatus());

        // 7. Start Out for Delivery
        OrderDto outForDeliveryOrder = deliveryService.startOutForDelivery(deliveryId, order.getId());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, outForDeliveryOrder.getOrderStatus());

        // Verify live tracking location
        Map<String, Object> liveLocation = deliveryService.getLiveDeliveryLocation(order.getId());
        assertNotNull(liveLocation);
        assertEquals(12.9360, liveLocation.get("latitude"));
        assertEquals(77.6250, liveLocation.get("longitude"));

        // 8. Complete Delivery
        OrderDto deliveredOrder = deliveryService.completeDelivery(deliveryId, order.getId());
        assertEquals(OrderStatus.DELIVERED, deliveredOrder.getOrderStatus());
        assertNotNull(deliveredOrder.getDeliveredAt());

        // Verify partner delivery counter incremented
        DeliveryProfileDto finalProfile = deliveryService.getOrCreateProfile(deliveryId);
        assertEquals(1, finalProfile.getTotalDeliveries());
    }
}
