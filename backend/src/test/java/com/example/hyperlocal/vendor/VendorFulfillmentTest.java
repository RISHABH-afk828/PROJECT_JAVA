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
import com.example.hyperlocal.delivery.dto.DeliveryStatusUpdateRequest;
import com.example.hyperlocal.delivery.service.DeliveryService;
import com.example.hyperlocal.payment.entity.PaymentMethod;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import com.example.hyperlocal.payment.service.RazorpaySignatureUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
        "app.razorpay.key-id=rzp_test_fixture123",
        "app.razorpay.key-secret=test_fixture_secret_xyz789"
})
@ActiveProfiles("h2")
@Transactional
public class VendorFulfillmentTest {

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

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
    private DeliveryService deliveryService;

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

        // Unpaid order (PAYMENT_PENDING) cannot be accepted by vendor
        ApiException unpaidEx = assertThrows(ApiException.class, () -> vendorOrderService.acceptOrder(vendorOwnerId, order1.getId()));
        assertTrue("INVALID_ORDER_STATE".equals(unpaidEx.getCode()) || "PAYMENT_NOT_CAPTURED".equals(unpaidEx.getCode()));

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(paymentService.getRestTemplate());
        mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"order_vf_rzp_001\",\"entity\":\"order\",\"amount\":10000,\"status\":\"created\"}", MediaType.APPLICATION_JSON));

        PaymentOrderResponse pOrder = paymentService.initiatePayment(customer.getId(), order1.getId());
        mockServer.verify();

        String sig1 = RazorpaySignatureUtil.calculateSignature(pOrder.getProviderOrderId(), "pay_test_001", keySecret);
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest();
        verifyReq.setOrderId(order1.getId());
        verifyReq.setRazorpayOrderId(pOrder.getProviderOrderId());
        verifyReq.setRazorpayPaymentId("pay_test_001");
        verifyReq.setRazorpaySignature(sig1);
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

        MockRestServiceServer mockServer2 = MockRestServiceServer.createServer(paymentService.getRestTemplate());
        mockServer2.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"order_vf_rzp_002\",\"entity\":\"order\",\"amount\":20000,\"status\":\"created\"}", MediaType.APPLICATION_JSON));

        PaymentOrderResponse pOrder2 = paymentService.initiatePayment(customer.getId(), order2.getId());
        mockServer2.verify();

        String sig2 = RazorpaySignatureUtil.calculateSignature(pOrder2.getProviderOrderId(), "pay_test_002", keySecret);
        PaymentVerifyRequest verifyReq2 = new PaymentVerifyRequest();
        verifyReq2.setOrderId(order2.getId());
        verifyReq2.setRazorpayOrderId(pOrder2.getProviderOrderId());
        verifyReq2.setRazorpayPaymentId("pay_test_002");
        verifyReq2.setRazorpaySignature(sig2);
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

    @Test
    void testCodOrderActionableBeforeCashCollectionWithoutBeingMarkedPaid() {
        User customer = userRepository.findByEmail("customer@hyperlocal.com").orElseThrow();
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        Long vendorOwnerId = vendor.getOwnerUserId();

        List<VendorDto> allVendors = vendorService.getNearbyVendors(null, null, "distance", 0, 10).getContent();
        VendorDto vendor2 = allVendors.stream().filter(v -> !v.getId().equals(vendor.getId())).findFirst().orElseThrow();
        Long vendor2OwnerId = vendor2.getOwnerUserId();

        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

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

        int initialStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);

        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(1);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        orderReq.setPaymentMethod(PaymentMethod.COD);
        OrderDto codOrder = checkoutService.createOrder(customer.getId(), orderReq);

        // COD order is placed in VENDOR_PENDING with CREATED payment status (not captured yet)
        assertEquals(OrderStatus.VENDOR_PENDING, codOrder.getOrderStatus());
        assertEquals(PaymentStatus.CREATED, codOrder.getPaymentStatus());
        assertEquals(PaymentMethod.COD, codOrder.getPaymentMethod());

        // Stock was atomically deducted on checkout
        int stockAfterCod = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(initialStock - 1, stockAfterCod);

        // Order appears in Incoming Orders queue for vendor
        Page<OrderDto> incoming = vendorOrderService.getVendorOrders(vendorOwnerId, OrderStatus.VENDOR_PENDING, 0, 10);
        assertTrue(incoming.getContent().stream().anyMatch(o -> o.getId().equals(codOrder.getId())),
                "Incoming Orders queue must contain actionable COD order");

        // Vendor 2 cannot accept Vendor 1's COD order (Cross-store isolation)
        assertThrows(ApiException.class, () -> vendorOrderService.acceptOrder(vendor2OwnerId, codOrder.getId()));

        // Vendor 1 accepts COD order before cash collection
        OrderDto accepted = vendorOrderService.acceptOrder(vendorOwnerId, codOrder.getId());
        assertEquals(OrderStatus.ACCEPTED, accepted.getOrderStatus());
        assertEquals(PaymentStatus.CREATED, accepted.getPaymentStatus(),
                "COD order must remain CREATED until cash collection at delivery");

        // Vendor marks preparing and ready
        OrderDto preparing = vendorOrderService.startPreparingOrder(vendorOwnerId, codOrder.getId());
        assertEquals(OrderStatus.PREPARING, preparing.getOrderStatus());

        OrderDto ready = vendorOrderService.markOrderReady(vendorOwnerId, codOrder.getId());
        assertEquals(OrderStatus.READY, ready.getOrderStatus());
    }

    @Test
    void testCodOrderRejectionRestoresDeductedStock() {
        User customer = userRepository.findByEmail("customer@hyperlocal.com").orElseThrow();
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        Long vendorOwnerId = vendor.getOwnerUserId();
        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

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

        int initialStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);

        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(3);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        orderReq.setPaymentMethod(PaymentMethod.COD);
        OrderDto codOrder = checkoutService.createOrder(customer.getId(), orderReq);

        int stockAfterOrder = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(initialStock - 3, stockAfterOrder);

        // Vendor rejects the COD order
        OrderDto rejected = vendorOrderService.rejectOrder(vendorOwnerId, codOrder.getId(), "Unable to fulfill COD order");
        assertEquals(OrderStatus.REJECTED, rejected.getOrderStatus());

        // Stock restored back to initial
        int restoredStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(initialStock, restoredStock, "Stock should be restored to initial level after COD rejection");

        // Verify movement log
        List<InventoryMovement> movements = movementRepository.findTop20ByProductIdOrderByCreatedAtDesc(product.getId());
        assertFalse(movements.isEmpty());
        assertEquals("CANCEL_RESTORE", movements.get(0).getReason());
        assertEquals(3, movements.get(0).getQuantityChange());
    }

    @Test
    void testCodOrderDeliveryCapturesPayment() {
        User customer = userRepository.findByEmail("customer@hyperlocal.com").orElseThrow();
        User deliveryPartner = userRepository.findByEmail("delivery@hyperlocal.com").orElseThrow();
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        Long vendorOwnerId = vendor.getOwnerUserId();
        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

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

        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(1);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        orderReq.setPaymentMethod(PaymentMethod.COD);
        OrderDto codOrder = checkoutService.createOrder(customer.getId(), orderReq);

        vendorOrderService.acceptOrder(vendorOwnerId, codOrder.getId());
        vendorOrderService.startPreparingOrder(vendorOwnerId, codOrder.getId());
        vendorOrderService.markOrderReady(vendorOwnerId, codOrder.getId());

        // Delivery partner flow
        deliveryService.getOrCreateProfile(deliveryPartner.getId());
        DeliveryStatusUpdateRequest statusReq = new DeliveryStatusUpdateRequest();
        statusReq.setIsOnline(true);
        deliveryService.updateOnlineStatus(deliveryPartner.getId(), statusReq);

        deliveryService.acceptOrder(deliveryPartner.getId(), codOrder.getId());
        deliveryService.confirmPickup(deliveryPartner.getId(), codOrder.getId());
        deliveryService.startOutForDelivery(deliveryPartner.getId(), codOrder.getId());

        // Complete delivery -> captures COD payment
        OrderDto delivered = deliveryService.completeDelivery(deliveryPartner.getId(), codOrder.getId());
        assertEquals(OrderStatus.DELIVERED, delivered.getOrderStatus());
        assertEquals(PaymentStatus.CAPTURED, delivered.getPaymentStatus(),
                "COD order payment status must transition to CAPTURED upon completed delivery");
    }
}
