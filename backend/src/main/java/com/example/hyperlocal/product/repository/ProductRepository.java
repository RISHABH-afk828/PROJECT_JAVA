package com.example.hyperlocal.product.repository;

import com.example.hyperlocal.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.vendorId = :vendorId AND p.deletedAt IS NULL AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:activeOnly = false OR p.active = true) AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> findVendorProducts(
            @Param("vendorId") Long vendorId,
            @Param("categoryId") Long categoryId,
            @Param("activeOnly") boolean activeOnly,
            @Param("query") String query,
            Pageable pageable);

    List<Product> findByVendorIdAndDeletedAtIsNull(Long vendorId);

    Optional<Product> findByIdAndVendorIdAndDeletedAtIsNull(Long id, Long vendorId);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);
}
