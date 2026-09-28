package com.example.hyperlocal.payment.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.payment.dto.PaymentDto;
import com.example.hyperlocal.payment.dto.PaymentOrderRequest;
import com.example.hyperlocal.payment.dto.PaymentOrderResponse;
import com.example.hyperlocal.payment.dto.PaymentVerifyRequest;
import com.example.hyperlocal.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentOrderRequest req) {
        PaymentOrderResponse response = paymentService.initiatePayment(principal.getId(), req.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse<PaymentDto>> verifyPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentVerifyRequest req) {
        PaymentDto payment = paymentService.verifyPayment(principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        Map<String, Object> result = paymentService.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
