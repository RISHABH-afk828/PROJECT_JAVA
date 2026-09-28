package com.example.hyperlocal.inventory.repository;

import com.example.hyperlocal.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdIn(Collection<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findWithLockByProductId(@Param("productId") Long productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds AND i.availableQuantity <= i.lowStockThreshold")
    List<Inventory> findLowStockInventories(@Param("productIds") Collection<Long> productIds);
}
