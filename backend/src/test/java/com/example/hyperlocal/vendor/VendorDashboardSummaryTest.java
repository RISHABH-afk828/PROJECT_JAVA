package com.example.hyperlocal.vendor;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.service.AddressService;
import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.service.CartService;
import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.service.CheckoutService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.delivery.dto.DeliveryStatusUpdateRequest;
import com.example.hyperlocal.delivery.service.DeliveryService;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.payment.dto.PaymentOrderResponse;
import com.example.hyperlocal.payment.dto.PaymentVerifyRequest;
import com.example.hyperlocal.payment.entity.PaymentMethod;
import com.example.hyperlocal.payment.service.PaymentService;
import com.example.hyperlocal.payment.service.RazorpaySignatureUtil;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.service.ProductService;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDashboardSummaryDto;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.service.VendorOrderService;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
public class VendorDashboardSummaryTest {

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

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
    private DeliveryService deliveryService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testDashboardSummaryInitialStateAndZeroGrowthHandling() {
        VendorDto vendor = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 1).getContent().get(0);
        Long vendorOwnerId = vendor.getOwnerUserId();

        VendorDashboardSummaryDto summary = vendorService.getDashboardSummary(vendorOwnerId);
        assertNotNull(summary);
        assertEquals(0, summary.getTodayOrders());
        assertEquals(0, summary.getYesterdayOrders());
        assertEquals(0, summary.getPendingOrders());
        assertEquals(0, summary.getCompletedOrdersToday());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getTodaySales()));
        assertNull(summary.getOrderGrowthPercent(), "Percent must be null when yesterday has 0 orders to avoid divide by zero");
        assertEquals("No orders yesterday", summary.getOrderGrowthLabel());
    }

    @Test
    void testDashboardSummaryWithPaidOrderAndStatusProgression() {
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

        // Add to cart & checkout
        AddCartItemRequest addReq = new AddCartItemRequest();
        addReq.setVendorId(vendor.getId());
        addReq.setProductId(product.getId());
        addReq.setQuantity(2);
        addReq.setReplaceCart(true);
        cartService.addItem(customer.getId(), addReq);

        CheckoutCreateRequest coReq = new CheckoutCreateRequest();
        coReq.setAddressId(address.getId());
        OrderDto createdOrder = checkoutService.createOrder(customer.getId(), coReq);

        // Before payment: Order is PAYMENT_PENDING, so pendingOrders is 0 and awaitingPaymentOrders is 1
        VendorDashboardSummaryDto beforePayment = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(0, beforePayment.getPendingOrders(), "Unpaid order must NOT count as pending actionable order");
        assertEquals(1, beforePayment.getAwaitingPaymentOrders(), "Unpaid order must count as awaiting payment");

        // Mock Razorpay and capture payment
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(paymentService.getRestTemplate());
        mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"order_dash_test_1\",\"entity\":\"order\",\"amount\":15000,\"status\":\"created\"}", MediaType.APPLICATION_JSON));

        PaymentOrderResponse pOrder = paymentService.initiatePayment(customer.getId(), createdOrder.getId());
        mockServer.verify();

        String sig = RazorpaySignatureUtil.calculateSignature(pOrder.getProviderOrderId(), "pay_dash_test_1", keySecret);
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest();
        verifyReq.setOrderId(createdOrder.getId());
        verifyReq.setRazorpayOrderId(pOrder.getProviderOrderId());
        verifyReq.setRazorpayPaymentId("pay_dash_test_1");
        verifyReq.setRazorpaySignature(sig);
        paymentService.verifyPayment(customer.getId(), verifyReq);

        // Verify summary after paid order created
        VendorDashboardSummaryDto afterPaid = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(1, afterPaid.getTodayOrders());
        assertEquals(1, afterPaid.getPendingOrders(), "New paid order is in VENDOR_PENDING awaiting vendor action");
        assertEquals(0, afterPaid.getAwaitingPaymentOrders(), "Captured order should no longer be awaiting payment");
        assertTrue(afterPaid.getTodaySales().compareTo(BigDecimal.ZERO) > 0, "Today sales must reflect paid order total");
        assertEquals(0, afterPaid.getCompletedOrdersToday());

        // Vendor accepts order -> status ACCEPTED (still pending vendor action)
        vendorOrderService.acceptOrder(vendorOwnerId, createdOrder.getId());
        VendorDashboardSummaryDto afterAccept = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(1, afterAccept.getPendingOrders());

        // Vendor marks preparing -> status PREPARING (still pending vendor action)
        vendorOrderService.startPreparingOrder(vendorOwnerId, createdOrder.getId());
        VendorDashboardSummaryDto afterPrep = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(1, afterPrep.getPendingOrders());

        // Vendor marks ready -> status READY (ready for pickup by delivery partner, no longer pending vendor action)
        vendorOrderService.markOrderReady(vendorOwnerId, createdOrder.getId());
        VendorDashboardSummaryDto afterReady = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(0, afterReady.getPendingOrders(), "Ready orders are awaiting delivery pickup, not vendor action");

        // Delivery partner initializes profile and goes online
        deliveryService.getOrCreateProfile(deliveryPartner.getId());
        DeliveryStatusUpdateRequest statusReq = new DeliveryStatusUpdateRequest();
        statusReq.setIsOnline(true);
        deliveryService.updateOnlineStatus(deliveryPartner.getId(), statusReq);

        // Delivery partner accepts, picks up, and delivers
        deliveryService.acceptOrder(deliveryPartner.getId(), createdOrder.getId());
        deliveryService.confirmPickup(deliveryPartner.getId(), createdOrder.getId());
        deliveryService.startOutForDelivery(deliveryPartner.getId(), createdOrder.getId());
        deliveryService.completeDelivery(deliveryPartner.getId(), createdOrder.getId());

        VendorDashboardSummaryDto afterDelivered = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(1, afterDelivered.getCompletedOrdersToday(), "Completed orders today must be incremented upon delivery");
        assertEquals(1, afterDelivered.getTodayOrders());
        assertEquals(0, afterDelivered.getPendingOrders());
    }

    @Test
    void testDashboardSummaryRejectsNonVendorAccount() {
        Long invalidOwnerId = 999999L;
        ApiException ex = assertThrows(ApiException.class, () -> vendorService.getDashboardSummary(invalidOwnerId));
        assertEquals("STORE_NOT_FOUND", ex.getCode());
    }

    @Test
    void testDashboardSummaryMixedCodAndOnlineOrders() {
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

        // 1. Create online order (unpaid)
        AddCartItemRequest onlineCart = new AddCartItemRequest();
        onlineCart.setVendorId(vendor.getId());
        onlineCart.setProductId(product.getId());
        onlineCart.setQuantity(1);
        onlineCart.setReplaceCart(true);
        cartService.addItem(customer.getId(), onlineCart);

        CheckoutCreateRequest onlineReq = new CheckoutCreateRequest();
        onlineReq.setAddressId(address.getId());
        onlineReq.setPaymentMethod(PaymentMethod.RAZORPAY);
        OrderDto onlineOrder = checkoutService.createOrder(customer.getId(), onlineReq);

        // 2. Create COD order
        AddCartItemRequest codCart = new AddCartItemRequest();
        codCart.setVendorId(vendor.getId());
        codCart.setProductId(product.getId());
        codCart.setQuantity(1);
        codCart.setReplaceCart(true);
        cartService.addItem(customer.getId(), codCart);

        CheckoutCreateRequest codReq = new CheckoutCreateRequest();
        codReq.setAddressId(address.getId());
        codReq.setPaymentMethod(PaymentMethod.COD);
        OrderDto codOrder = checkoutService.createOrder(customer.getId(), codReq);

        // Check metrics:
        // todayOrders = 2
        // pendingOrders = 1 (only COD is actionable; online is awaiting payment)
        // awaitingPaymentOrders = 1 (the online order)
        VendorDashboardSummaryDto summary = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(2, summary.getTodayOrders(), "Both orders should count towards today's total");
        assertEquals(1, summary.getPendingOrders(), "Only the COD order is actionable before payment capture");
        assertEquals(1, summary.getAwaitingPaymentOrders(), "The unpaid online order should count as awaiting payment");

        // Vendor accepts the COD order -> pendingOrders still 1 (in ACCEPTED)
        vendorOrderService.acceptOrder(vendorOwnerId, codOrder.getId());
        VendorDashboardSummaryDto afterAcceptCod = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(1, afterAcceptCod.getPendingOrders());

        // Now capture payment on the online order
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(paymentService.getRestTemplate());
        mockServer.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"order_dash_mix_1\",\"entity\":\"order\",\"amount\":15000,\"status\":\"created\"}", MediaType.APPLICATION_JSON));

        PaymentOrderResponse pOrder = paymentService.initiatePayment(customer.getId(), onlineOrder.getId());
        mockServer.verify();

        String sig = RazorpaySignatureUtil.calculateSignature(pOrder.getProviderOrderId(), "pay_dash_mix_1", keySecret);
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest();
        verifyReq.setOrderId(onlineOrder.getId());
        verifyReq.setRazorpayOrderId(pOrder.getProviderOrderId());
        verifyReq.setRazorpayPaymentId("pay_dash_mix_1");
        verifyReq.setRazorpaySignature(sig);
        paymentService.verifyPayment(customer.getId(), verifyReq);

        // After online payment capture:
        // pendingOrders = 2 (COD in ACCEPTED + Online in VENDOR_PENDING)
        // awaitingPaymentOrders = 0
        VendorDashboardSummaryDto afterOnlinePaid = vendorService.getDashboardSummary(vendorOwnerId);
        assertEquals(2, afterOnlinePaid.getPendingOrders());
        assertEquals(0, afterOnlinePaid.getAwaitingPaymentOrders());
    }
}
