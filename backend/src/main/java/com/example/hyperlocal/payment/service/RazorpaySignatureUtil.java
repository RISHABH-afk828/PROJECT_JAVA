package com.example.hyperlocal.payment.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class RazorpaySignatureUtil {

    private RazorpaySignatureUtil() {
    }

    public static String calculateSignature(String orderId, String paymentId, String secret) {
        String data = orderId + "|" + paymentId;
        return calculateHmacSha256(data, secret);
    }

    public static boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        if (signature == null || secret == null) {
            return false;
        }
        String expected = calculateSignature(orderId, paymentId, secret);
        return expected.equalsIgnoreCase(signature.trim());
    }

    public static String calculateWebhookSignature(String rawPayload, String secret) {
        return calculateHmacSha256(rawPayload, secret);
    }

    public static boolean verifyWebhookSignature(String rawPayload, String signature, String secret) {
        if (signature == null || secret == null || rawPayload == null) {
            return false;
        }
        String expected = calculateWebhookSignature(rawPayload, secret);
        return expected.equalsIgnoreCase(signature.trim());
    }

    private static String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
