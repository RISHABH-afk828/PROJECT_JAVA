package com.example.hyperlocal.product.controller;

import com.example.hyperlocal.common.response.ApiResponse;
import com.example.hyperlocal.common.security.UserPrincipal;
import com.example.hyperlocal.product.dto.ProductDto;
import com.example.hyperlocal.product.dto.ProductRequest;
import com.example.hyperlocal.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/vendors/{vendorId}/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getVendorProducts(
            @PathVariable Long vendorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "true") boolean availableOnly,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductDto> productPage = productService.getVendorProducts(vendorId, categoryId, availableOnly, q, page, size);
        Map<String, Object> meta = Map.of(
                "page", productPage.getNumber(),
                "size", productPage.getSize(),
                "totalElements", productPage.getTotalElements(),
                "totalPages", productPage.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(productPage.getContent(), meta));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping("/vendors/{vendorId}/products")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vendorId,
            @Valid @RequestBody ProductRequest req) {
        ProductDto created = productService.createProduct(principal.getId(), vendorId, req);
        return new ResponseEntity<>(ApiResponse.success(created), HttpStatus.CREATED);
    }

    @PatchMapping("/vendors/{vendorId}/products/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vendorId,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest req) {
        ProductDto updated = productService.updateProduct(principal.getId(), vendorId, id, req);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/vendors/{vendorId}/products/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vendorId,
            @PathVariable Long id) {
        productService.softDeleteProduct(principal.getId(), vendorId, id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Product archived successfully")));
    }

    @PostMapping("/vendors/{vendorId}/products/{id}/activate")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> activateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vendorId,
            @PathVariable Long id) {
        productService.setProductStatus(principal.getId(), vendorId, id, true);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Product activated successfully")));
    }

    @PostMapping("/vendors/{vendorId}/products/{id}/deactivate")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> deactivateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vendorId,
            @PathVariable Long id) {
        productService.setProductStatus(principal.getId(), vendorId, id, false);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Product deactivated successfully")));
    }
}
