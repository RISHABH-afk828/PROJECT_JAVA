package com.example.hyperlocal.checkout.controller;

import com.example.hyperlocal.checkout.dto.CheckoutCreateRequest;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteDto;
import com.example.hyperlocal.checkout.dto.CheckoutQuoteRequest;
import com.example.hyperlocal.checkout.service.CheckoutService;
import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.order.dto.OrderDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<CheckoutQuoteDto>> getQuote(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutQuoteRequest req) {
        CheckoutQuoteDto quote = checkoutService.calculateQuote(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(quote));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutCreateRequest req) {
        OrderDto order = checkoutService.createOrder(principal.getId(), req);
        return new ResponseEntity<>(ApiResponse.success(order), HttpStatus.CREATED);
    }
}
