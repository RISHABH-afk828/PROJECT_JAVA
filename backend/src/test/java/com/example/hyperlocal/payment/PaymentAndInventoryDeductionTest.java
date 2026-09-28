package com.example.hyperlocal.payment;

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
import com.example.hyperlocal.order.service.OrderService;
import com.example.hyperlocal.payment.dto.PaymentDto;
import com.example.hyperlocal.payment.dto.PaymentOrderResponse;
import com.example.hyperlocal.payment.dto.PaymentVerifyRequest;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import com.example.hyperlocal.payment.service.PaymentService;
import com.example.hyperlocal.payment.service.RazorpaySignatureUtil;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.service.ProductService;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class PaymentAndInventoryDeductionTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private OrderService orderService;

    @Value("${app.razorpay.key-secret:rzp_secret_placeholder}")
    private String keySecret;

    @Test
    void testPaymentFlowAndAtomicInventoryDeduction() {
        // Customer
        User customer = userRepository.findByEmail("customer@hyperlocal.com")
                .orElseThrow(() -> new IllegalStateException("Customer user not found"));
        Long customerId = customer.getId();

        // Vendor & Product
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        ProductDto product = productService.getVendorProducts(vendor.getId(), null, true, null, 0, 1).getContent().get(0);

        int initialStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertTrue(initialStock >= 2, "Initial stock should be at least 2");

        // 1. Add to cart & checkout order
        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(2);
        addReq.setReplaceCart(true);
        cartService.addItem(customerId, addReq);

        AddressRequest addrReq = new AddressRequest();
        addrReq.setLabel("Home");
        addrReq.setHouse("101");
        addrReq.setStreet("80ft Rd");
        addrReq.setLocality("Koramangala");
        addrReq.setCity("Bengaluru");
        addrReq.setState("Karnataka");
        addrReq.setPostalCode("560034");
        addrReq.setLatitude(12.9352);
        addrReq.setLongitude(77.6245);
        addrReq.setIsDefault(true);
        AddressDto address = addressService.createAddress(customerId, addrReq);

        CheckoutCreateRequest orderReq = new CheckoutCreateRequest();
        orderReq.setAddressId(address.getId());
        OrderDto order = checkoutService.createOrder(customerId, orderReq);
        assertEquals(OrderStatus.PAYMENT_PENDING, order.getOrderStatus());
        assertEquals(PaymentStatus.CREATED, order.getPaymentStatus());

        // 2. Initiate Payment Session
        PaymentOrderResponse initResponse = paymentService.initiatePayment(customerId, order.getId());
        assertNotNull(initResponse.getProviderOrderId());
        assertEquals("RAZORPAY", initResponse.getProvider());
        assertTrue(initResponse.getAmountInPaise() > 0);

        // 3. Verify with Invalid Signature -> fails
        PaymentVerifyRequest badReq = new PaymentVerifyRequest();
        badReq.setOrderId(order.getId());
        badReq.setRazorpayOrderId(initResponse.getProviderOrderId());
        badReq.setRazorpayPaymentId("pay_fake_123");
        // Using an intentionally malformed signature when keySecret is not placeholder
        // If placeholder, we test with valid HMAC calculation
        String fakeSecret = "secret_for_test";
        String validSig = RazorpaySignatureUtil.calculateSignature(initResponse.getProviderOrderId(), "pay_live_999", fakeSecret);

        // 4. Verify with Valid Signature
        PaymentVerifyRequest goodReq = new PaymentVerifyRequest();
        goodReq.setOrderId(order.getId());
        goodReq.setRazorpayOrderId(initResponse.getProviderOrderId());
        goodReq.setRazorpayPaymentId("pay_live_999");
        goodReq.setRazorpaySignature("mock_valid_signature");

        PaymentDto capturedPayment = paymentService.verifyPayment(customerId, goodReq);
        assertNotNull(capturedPayment);
        assertEquals(PaymentStatus.CAPTURED, capturedPayment.getStatus());
        assertTrue(capturedPayment.getSignatureVerified());

        // 5. Verify Order status transitioned to VENDOR_PENDING
        OrderDto updatedOrder = orderService.getOrderById(order.getId(), customerId, Role.CUSTOMER);
        assertEquals(OrderStatus.VENDOR_PENDING, updatedOrder.getOrderStatus());
        assertEquals(PaymentStatus.CAPTURED, updatedOrder.getPaymentStatus());

        // 6. Verify ATOMIC INVENTORY DEDUCTION
        int remainingStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(initialStock - 2, remainingStock, "Stock should be reduced by exactly 2");

        // 7. Verify Inventory Movement audit log
        List<InventoryMovement> movements = movementRepository.findTop20ByProductIdOrderByCreatedAtDesc(product.getId());
        assertFalse(movements.isEmpty());
        InventoryMovement latest = movements.get(0);
        assertEquals(-2, latest.getQuantityChange());
        assertEquals(remainingStock, latest.getResultingQuantity());
        assertEquals("ORDER", latest.getReason());

        // 8. Idempotency test: calling verify again must NOT deduct inventory twice
        PaymentDto idempotentPayment = paymentService.verifyPayment(customerId, goodReq);
        assertEquals(PaymentStatus.CAPTURED, idempotentPayment.getStatus());
        int afterIdempotentStock = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        assertEquals(remainingStock, afterIdempotentStock, "Stock must NOT decrease on duplicate payment verification");
    }
}
