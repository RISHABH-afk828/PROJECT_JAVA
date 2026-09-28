package com.example.hyperlocal.order;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.service.AddressService;
import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.dto.CartDto;
import com.example.hyperlocal.cart.dto.UpdateCartItemRequest;
import com.example.hyperlocal.cart.service.CartService;
import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteDto;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteRequest;
import com.example.hyperlocal.checkout.service.CheckoutService;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.order.dto.OrderDto;
import com.example.hyperlocal.order.dto.OrderStatusHistoryDto;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.order.service.OrderService;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.service.ProductService;
import com.example.hyperlocal.user.entity.Role;
import com.example.hyperlocal.user.entity.User;
import com.example.hyperlocal.user.repository.UserRepository;
import com.example.hyperlocal.vendor.dto.VendorDto;
import com.example.hyperlocal.vendor.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class CartAndCheckoutTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private ProductService productService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCartLifecycleAndCheckoutFlow() {
        // Customer seeded: customer@hyperlocal.com
        User customer = userRepository.findByEmail("customer@hyperlocal.com")
                .orElseThrow(() -> new IllegalStateException("Customer user not found"));
        Long customerId = customer.getId();

        // 1. Get 2 different vendors from seed
        List<VendorDto> vendors = vendorService.getNearbyVendors(12.9352, 77.6245, "distance", 0, 10).getContent();
        assertTrue(vendors.size() >= 2, "Need at least 2 vendors to test single-vendor invariant");

        VendorDto vendor1 = vendors.get(0);
        VendorDto vendor2 = vendors.get(1);

        List<ProductDto> v1Products = productService.getVendorProducts(vendor1.getId(), null, true, null, 0, 10).getContent();
        assertFalse(v1Products.isEmpty(), "Vendor 1 should have products");
        ProductDto v1P1 = v1Products.get(0);

        List<ProductDto> v2Products = productService.getVendorProducts(vendor2.getId(), null, true, null, 0, 10).getContent();
        assertFalse(v2Products.isEmpty(), "Vendor 2 should have products");
        ProductDto v2P1 = v2Products.get(0);

        // 2. Add item from Vendor 1 to cart
        AddCartItemRequest addReq1 = new AddCartItemRequest();
        addReq1.setVendorId(vendor1.getId());
        addReq1.setProductId(v1P1.getId());
        addReq1.setQuantity(2);

        CartDto cart = cartService.addItem(customerId, addReq1);
        assertEquals(vendor1.getId(), cart.getVendorId());
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(v1P1.getPrice().multiply(BigDecimal.valueOf(2)), cart.getSubtotal());

        // 3. Test Single-Vendor Cart Conflict (attempt adding item from Vendor 2)
        AddCartItemRequest addReq2 = new AddCartItemRequest();
        addReq2.setVendorId(vendor2.getId());
        addReq2.setProductId(v2P1.getId());
        addReq2.setQuantity(1);

        ApiException conflictEx = assertThrows(ApiException.class, () -> cartService.addItem(customerId, addReq2));
        assertEquals("CART_VENDOR_CONFLICT", conflictEx.getCode());

        // 4. Test Single-Vendor Conflict Resolution with replaceCart = true
        addReq2.setReplaceCart(true);
        CartDto replacedCart = cartService.addItem(customerId, addReq2);
        assertEquals(vendor2.getId(), replacedCart.getVendorId());
        assertEquals(1, replacedCart.getItems().size());
        assertEquals(v2P1.getId(), replacedCart.getItems().get(0).getProductId());

        // Reset back to vendor1 with 2 items for checkout test
        AddCartItemRequest reAdd1 = new AddCartItemRequest();
        reAdd1.setVendorId(vendor1.getId());
        reAdd1.setProductId(v1P1.getId());
        reAdd1.setQuantity(2);
        reAdd1.setReplaceCart(true);
        cart = cartService.addItem(customerId, reAdd1);
        assertEquals(vendor1.getId(), cart.getVendorId());

        // 5. Update item quantity and test insufficient stock
        Long cartItemId = cart.getItems().get(0).getId();
        UpdateCartItemRequest updateReq = new UpdateCartItemRequest();
        updateReq.setQuantity(99999); // Exceeds available stock

        ApiException stockEx = assertThrows(ApiException.class, () -> cartService.updateItemQuantity(customerId, cartItemId, updateReq.getQuantity()));
        assertEquals("INSUFFICIENT_STOCK", stockEx.getCode());

        // 6. Test Address Creation (nearby Bangalore address)
        AddressRequest addrReq = new AddressRequest();
        addrReq.setLabel("Apartment");
        addrReq.setHouse("Flat 402");
        addrReq.setStreet("80 Feet Road");
        addrReq.setLocality("Koramangala");
        addrReq.setCity("Bengaluru");
        addrReq.setState("Karnataka");
        addrReq.setPostalCode("560034");
        addrReq.setLatitude(12.9352);
        addrReq.setLongitude(77.6245);
        addrReq.setIsDefault(true);

        AddressDto address = addressService.createAddress(customerId, addrReq);
        assertNotNull(address.getId());

        // 7. Calculate Checkout Quote
        CheckoutQuoteRequest quoteReq = new CheckoutQuoteRequest();
        quoteReq.setAddressId(address.getId());

        CheckoutQuoteDto quote = checkoutService.calculateQuote(customerId, quoteReq);
        assertNotNull(quote);
        assertEquals(cart.getSubtotal(), quote.getSubtotal());
        assertNotNull(quote.getDeliveryFee());
        assertTrue(quote.getDeliveryFee().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(quote.getTax());
        assertTrue(quote.getTotal().compareTo(quote.getSubtotal()) > 0);
        assertEquals(vendor1.getStoreName(), quote.getVendorName());

        // 8. Test Location Not Serviceable (Address far away in Mumbai)
        AddressRequest farAddrReq = new AddressRequest();
        farAddrReq.setLabel("Far Office");
        farAddrReq.setHouse("10");
        farAddrReq.setStreet("Marine Drive");
        farAddrReq.setLocality("Nariman Point");
        farAddrReq.setCity("Mumbai");
        farAddrReq.setState("Maharashtra");
        farAddrReq.setPostalCode("400021");
        farAddrReq.setLatitude(18.9220);
        farAddrReq.setLongitude(72.8347);

        AddressDto farAddress = addressService.createAddress(customerId, farAddrReq);
        CheckoutQuoteRequest farQuoteReq = new CheckoutQuoteRequest();
        farQuoteReq.setAddressId(farAddress.getId());

        ApiException radiusEx = assertThrows(ApiException.class, () -> checkoutService.calculateQuote(customerId, farQuoteReq));
        assertEquals("LOCATION_NOT_SERVICEABLE", radiusEx.getCode());

        // 9. Create Order
        CheckoutCreateRequest createReq = new CheckoutCreateRequest();
        createReq.setAddressId(address.getId());

        OrderDto order = checkoutService.createOrder(customerId, createReq);
        assertNotNull(order.getId());
        assertNotNull(order.getOrderNumber());
        assertEquals(OrderStatus.PAYMENT_PENDING, order.getOrderStatus());
        assertEquals(PaymentStatus.CREATED, order.getPaymentStatus());
        assertEquals(1, order.getItems().size());
        assertEquals(quote.getTotal(), order.getTotal());

        // Verify cart is now cleared
        CartDto clearedCart = cartService.getCart(customerId);
        assertTrue(clearedCart.getItems().isEmpty());
        assertNull(clearedCart.getVendorId());

        // 10. Verify Order retrieval and status timeline
        OrderDto retrieved = orderService.getOrderById(order.getId(), customerId, Role.CUSTOMER);
        assertEquals(order.getOrderNumber(), retrieved.getOrderNumber());

        List<OrderStatusHistoryDto> history = orderService.getOrderStatusHistory(order.getId(), customerId, Role.CUSTOMER);
        assertFalse(history.isEmpty());
        assertEquals(OrderStatus.PAYMENT_PENDING, history.get(0).getToStatus());
    }
}
