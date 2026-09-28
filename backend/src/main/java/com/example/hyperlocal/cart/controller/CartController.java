package com.example.hyperlocal.cart.controller;

import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.dto.CartDto;
import com.example.hyperlocal.cart.dto.UpdateCartItemRequest;
import com.example.hyperlocal.cart.service.CartService;
import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDto>> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        CartDto cart = cartService.getCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDto>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest req) {
        CartDto cart = cartService.addItem(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartDto>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest req) {
        CartDto cart = cartService.updateItemQuantity(principal.getId(), id, req.getQuantity());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartDto>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        CartDto cart = cartService.removeItem(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartDto>> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        CartDto cart = cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCart(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = cartService.validateCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
