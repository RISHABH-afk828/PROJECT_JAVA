package com.example.hyperlocal.order.repository;

import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findWithLockById(@Param("id") Long id);

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<Order> findByCustomerIdAndOrderStatusInOrderByCreatedAtDesc(Long customerId, List<OrderStatus> statuses);

    Page<Order> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);

    Page<Order> findByVendorIdAndOrderStatusOrderByCreatedAtDesc(Long vendorId, OrderStatus status, Pageable pageable);

    Page<Order> findByDeliveryPartnerIdOrderByCreatedAtDesc(Long deliveryPartnerId, Pageable pageable);

    List<Order> findByOrderStatus(OrderStatus orderStatus);
}
