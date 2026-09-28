package com.example.hyperlocal.vendor;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.service.AddressService;
import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.service.CartService;
import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.service.CheckoutService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.entity.InventoryMovement;
import com.example.hyperlocal.inventory.repository.InventoryMovementRepository;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class VendorFulfillmentTest {

    @Autowired
    private VendorOrderService vendorOrderService;

    @Autowired
    private VendorService vendorService;

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

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Test
    void testVendorFulfillmentLifecycleAndRejectionRestoration() {
        // Customer
        User customer = userRepository.findByEmail("customer@hyperlocal.com").orElseThrow();
        // Vendor 1 (Fresh Mart, owner: Suresh FreshMart)
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        Long vendorOwnerId = vendor.getOwnerUserId();

        // Vendor 2 (Second vendor for unauthorized access check)
        List<VendorDto> allVendors = vendorService.getNearbyVendors(null, null, "distance", 0, 10).getContent();
        VendorDto vendor2 = allVendors.stream().filter(v -> !v.getId().equals(vendor.getId())).findFirst().orElseThrow();
        Long vendor2OwnerId = vendor2.getOwnerUserId();

        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

        // Setup Customer address
        AddressRequest addrReq = new AddressRequest();
        addrReq.setLabel("Home");
        addrReq.setHouse("10");
        addrReq.setStreet("80ft Rd");
        addrReq.setLocality("Koramangala");
        addrReq.setCity("Bengaluru");
        addrReq.setState("Karnataka");
        addrReq.setPostalCode("560034");
        addrReq.setLatitude(12.9352);
        addrReq.setLongitude(77.6245);
        AddressDto address = addressService.createAddress(customer.getId(), addrReq);

        // 1. Create Order 1 and capture payment (Transitions to VENDOR_PENDING)
        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(1);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        OrderDto order1 = checkoutService.createOrder(customer.getId(), orderReq);

        PaymentOrderResponse pOrder = paymentService.initiatePayment(customer.getId(), order1.getId());
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest();
        verifyReq.setOrderId(order1.getId());
        verifyReq.setRazorpayOrderId(pOrder.getProviderOrderId());
        verifyReq.setRazorpayPaymentId("pay_test_001");
        verifyReq.setRazorpaySignature("mock_valid_signature");
        paymentService.verifyPayment(customer.getId(), verifyReq);

        // 2. Vendor fetches orders queue
        Page<OrderDto> pendingOrders = vendorOrderService.getVendorOrders(
                vendorOwnerId, OrderStatus.VENDOR_PENDING, 0, 10);
        assertFalse(pendingOrders.isEmpty());
        assertTrue(pendingOrders.getContent().stream().anyMatch(o -> o.getId().equals(order1.getId())));

        // 3. Test unauthorized vendor trying to accept order1
        assertThrows(ApiException.class, () -> vendorOrderService.acceptOrder(vendor2OwnerId, order1.getId()),
                "Vendor 2 should not be able to accept Vendor 1's orders");

        // 4. Vendor 1 accepts order
        OrderDto accepted = vendorOrderService.acceptOrder(vendorOwnerId, order1.getId());
        assertEquals(OrderStatus.ACCEPTED, accepted.getOrderStatus());
        assertNotNull(accepted.getAcceptedAt());

        // 5. Vendor starts preparing
        OrderDto preparing = vendorOrderService.startPreparingOrder(vendorOwnerId, order1.getId());
        assertEquals(OrderStatus.PREPARING, preparing.getOrderStatus());

        // 6. Vendor marks ready for pickup
        OrderDto ready = vendorOrderService.markOrderReady(vendorOwnerId, order1.getId());
        assertEquals(OrderStatus.READY, ready.getOrderStatus());
        assertNotNull(ready.getPreparedAt());

        // 7. Test Order Rejection & Inventory Restoration
        int stockBeforeOrder2 = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);

        AddCartItemRequest addReq2 = new AddCartItemRequest();
        addReq2.setVendorId(vendor.getId());
        addReq2.setProductId(product.getId());
        addReq2.setQuantity(2);
        addReq2.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq2);

        OrderDto order2 = checkoutService.createOrder(customer.getId(), orderReq);
        PaymentOrderResponse pOrder2 = paymentService.initiatePayment(customer.getId(), order2.getId());
        PaymentVerifyRequest verifyReq2 = new PaymentVerifyRequest();
        verifyReq2.setOrderId(order2.getId());
        verifyReq2.setRazorpayOrderId(pOrder2.getProviderOrderId());
        verifyReq2.setRazorpayPaymentId("pay_test_002");
        verifyReq2.setRazorpaySignature("mock_valid_signature");
        paymentService.verifyPayment(customer.getId(), verifyReq2);

        int stockAfterOrder2 = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(stockBeforeOrder2 - 2, stockAfterOrder2, "Stock should have decreased by 2");

        // Vendor rejects order2
        OrderDto rejected = vendorOrderService.rejectOrder(
                vendorOwnerId, order2.getId(), "Kitchen closing early due to emergency maintenance");
        assertEquals(OrderStatus.REJECTED, rejected.getOrderStatus());

        // Verify stock is restored
        int restoredStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(stockBeforeOrder2, restoredStock, "Stock should be restored to pre-order levels after rejection");

        // Verify audit log has CANCEL_RESTORE
        List<InventoryMovement> movements = movementRepository.findTop20ByProductIdOrderByCreatedAtDesc(product.getId());
        assertFalse(movements.isEmpty());
        assertEquals("CANCEL_RESTORE", movements.get(0).getReason());
        assertEquals(2, movements.get(0).getQuantityChange());
    }
}
