package com.example.hyperlocal.cart.service;

import com.example.hyperlocal.cart.dto.AddCartItemRequest;
import com.example.hyperlocal.cart.dto.CartDto;
import com.example.hyperlocal.cart.dto.CartItemDto;
import com.example.hyperlocal.cart.entity.Cart;
import com.example.hyperlocal.cart.entity.CartItem;
import com.example.hyperlocal.cart.repository.CartItemRepository;
import com.example.hyperlocal.cart.repository.CartRepository;
import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.inventory.entity.Inventory;
import com.example.hyperlocal.inventory.repository.InventoryRepository;
import com.example.hyperlocal.product.entity.Product;
import com.example.hyperlocal.product.repository.ProductRepository;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final VendorRepository vendorRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            VendorRepository vendorRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public CartDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getVendorId() == null) {
            return emptyCartDto(cart != null ? cart.getId() : null);
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            return emptyCartDto(cart.getId());
        }

        return buildCartDto(cart, items);
    }

    @Transactional
    public CartDto addItem(Long userId, AddCartItemRequest req) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(req.getProductId())
                .orElseThrow(() -> new ApiException("PRODUCT_NOT_FOUND", "Product not found", HttpStatus.NOT_FOUND));

        if (!product.getActive()) {
            throw new ApiException("PRODUCT_INACTIVE", "This product is currently unavailable", HttpStatus.BAD_REQUEST);
        }

        if (!product.getVendorId().equals(req.getVendorId())) {
            throw new ApiException("INVALID_VENDOR_PRODUCT", "Product does not belong to specified store", HttpStatus.BAD_REQUEST);
        }

        Vendor vendor = vendorRepository.findById(req.getVendorId())
                .orElseThrow(() -> new ApiException("VENDOR_NOT_FOUND", "Store not found", HttpStatus.NOT_FOUND));

        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new ApiException("VENDOR_INACTIVE", "Store is currently inactive", HttpStatus.BAD_REQUEST);
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId, req.getVendorId())));

        // Check single-vendor constraint
        if (cart.getVendorId() != null && !cart.getVendorId().equals(req.getVendorId())) {
            List<CartItem> existingItems = cartItemRepository.findByCartId(cart.getId());
            if (!existingItems.isEmpty()) {
                if (Boolean.TRUE.equals(req.getReplaceCart())) {
                    cartItemRepository.deleteByCartId(cart.getId());
                    cart.setVendorId(req.getVendorId());
                    cartRepository.save(cart);
                } else {
                    Vendor existingVendor = vendorRepository.findById(cart.getVendorId()).orElse(null);
                    String existingVendorName = existingVendor != null ? existingVendor.getStoreName() : "another store";
                    throw new ApiException("CART_VENDOR_CONFLICT",
                            "Your cart contains items from " + existingVendorName + ". Clear cart to add items from this store.",
                            HttpStatus.CONFLICT,
                            Map.of(
                                    "cartVendorId", cart.getVendorId(),
                                    "cartVendorName", existingVendorName,
                                    "newVendorId", req.getVendorId(),
                                    "newVendorName", vendor.getStoreName()
                            ));
                }
            } else {
                cart.setVendorId(req.getVendorId());
                cartRepository.save(cart);
            }
        } else if (cart.getVendorId() == null) {
            cart.setVendorId(req.getVendorId());
            cartRepository.save(cart);
        }

        // Validate inventory
        Inventory inventory = inventoryRepository.findByProductId(req.getProductId()).orElse(null);
        int available = inventory != null ? inventory.getAvailableQuantity() : 0;

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), req.getProductId());
        int targetQuantity = req.getQuantity() + (existingItemOpt.isPresent() ? existingItemOpt.get().getQuantity() : 0);

        if (targetQuantity > available) {
            throw new ApiException("INSUFFICIENT_STOCK",
                    "Only " + available + " units available in stock",
                    HttpStatus.BAD_REQUEST,
                    Map.of("available", available, "requested", targetQuantity));
        }

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(targetQuantity);
            item.setPriceSnapshot(product.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart.getId(), product.getId(), req.getQuantity(), product.getPrice());
            cartItemRepository.save(newItem);
        }

        return getCart(userId);
    }

    @Transactional
    public CartDto updateItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ApiException("CART_ITEM_NOT_FOUND", "Item not found in cart", HttpStatus.NOT_FOUND));

        if (!item.getCartId().equals(cart.getId())) {
            throw new ApiException("ACCESS_DENIED", "Cart item does not belong to your cart", HttpStatus.FORBIDDEN);
        }

        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(item);
            List<CartItem> remaining = cartItemRepository.findByCartId(cart.getId());
            if (remaining.isEmpty()) {
                cart.setVendorId(null);
                cartRepository.save(cart);
            }
            return getCart(userId);
        }

        // Check stock
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).orElse(null);
        int available = inventory != null ? inventory.getAvailableQuantity() : 0;
        if (quantity > available) {
            throw new ApiException("INSUFFICIENT_STOCK",
                    "Only " + available + " units available in stock",
                    HttpStatus.BAD_REQUEST,
                    Map.of("available", available, "requested", quantity));
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartDto removeItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ApiException("CART_ITEM_NOT_FOUND", "Item not found in cart", HttpStatus.NOT_FOUND));

        if (!item.getCartId().equals(cart.getId())) {
            throw new ApiException("ACCESS_DENIED", "Cart item does not belong to your cart", HttpStatus.FORBIDDEN);
        }

        cartItemRepository.delete(item);

        List<CartItem> remaining = cartItemRepository.findByCartId(cart.getId());
        if (remaining.isEmpty()) {
            cart.setVendorId(null);
            cartRepository.save(cart);
        }

        return getCart(userId);
    }

    @Transactional
    public CartDto clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setVendorId(null);
            cartRepository.save(cart);
            return emptyCartDto(cart.getId());
        }
        return emptyCartDto(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateCart(Long userId) {
        CartDto cartDto = getCart(userId);
        boolean hasOutOfStock = cartDto.getItems().stream().anyMatch(CartItemDto::getIsOutOfStock);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", !hasOutOfStock && !cartDto.getItems().isEmpty());
        result.put("cart", cartDto);
        result.put("hasOutOfStock", hasOutOfStock);
        return result;
    }

    private CartDto buildCartDto(Cart cart, List<CartItem> items) {
        Vendor vendor = vendorRepository.findById(cart.getVendorId()).orElse(null);
        String vendorName = vendor != null ? vendor.getStoreName() : "Unknown Store";

        List<Long> productIds = items.stream().map(CartItem::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));

        List<CartItemDto> itemDtos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : items) {
            Product product = productMap.get(item.getProductId());
            Inventory inventory = inventoryMap.get(item.getProductId());

            if (product == null || !product.getActive()) {
                continue;
            }

            int available = inventory != null ? inventory.getAvailableQuantity() : 0;
            boolean isOutOfStock = available < item.getQuantity();

            BigDecimal price = product.getPrice();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

            CartItemDto itemDto = new CartItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(product.getId());
            itemDto.setProductName(product.getName());
            itemDto.setUnit(product.getUnit());
            itemDto.setUnitPrice(price);
            itemDto.setQuantity(item.getQuantity());
            itemDto.setLineTotal(lineTotal);
            itemDto.setAvailableQuantity(available);
            itemDto.setIsOutOfStock(isOutOfStock);
            itemDto.setImageUrl(product.getImageUrl());

            itemDtos.add(itemDto);
            subtotal = subtotal.add(lineTotal);
            totalItems += item.getQuantity();
        }

        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setVendorId(cart.getVendorId());
        dto.setVendorName(vendorName);
        dto.setItems(itemDtos);
        dto.setSubtotal(subtotal);
        dto.setTotalItems(totalItems);
        return dto;
    }

    private CartDto emptyCartDto(Long cartId) {
        CartDto dto = new CartDto();
        dto.setId(cartId);
        dto.setVendorId(null);
        dto.setVendorName(null);
        dto.setItems(new ArrayList<>());
        dto.setSubtotal(BigDecimal.ZERO);
        dto.setTotalItems(0);
        return dto;
    }
}
