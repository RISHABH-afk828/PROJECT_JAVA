package com.example.hyperlocal.order.repository;

import com.example.hyperlocal.order.entity.Order;
import com.example.hyperlocal.order.entity.OrderStatus;
import com.example.hyperlocal.payment.entity.PaymentMethod;
import com.example.hyperlocal.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
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
    
    Page<Order> findByVendorIdAndOrderStatusInOrderByCreatedAtDesc(Long vendorId, List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.vendorId = :vendorId AND o.orderStatus IN :statuses AND (o.paymentMethod = com.example.hyperlocal.payment.entity.PaymentMethod.COD OR o.paymentStatus = com.example.hyperlocal.payment.entity.PaymentStatus.CAPTURED) ORDER BY o.createdAt DESC")
    Page<Order> findIncomingOrdersForVendor(
            @Param("vendorId") Long vendorId,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);

    long countByVendorIdAndOrderStatus(Long vendorId, OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.orderStatus IN :statuses AND (o.paymentMethod = com.example.hyperlocal.payment.entity.PaymentMethod.COD OR o.paymentStatus = com.example.hyperlocal.payment.entity.PaymentStatus.CAPTURED)")
    long countPendingFulfillmentOrdersForVendor(
            @Param("vendorId") Long vendorId,
            @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND (o.paymentMethod IS NULL OR o.paymentMethod != com.example.hyperlocal.payment.entity.PaymentMethod.COD) AND (o.orderStatus = com.example.hyperlocal.order.entity.OrderStatus.PAYMENT_PENDING OR o.paymentStatus != com.example.hyperlocal.payment.entity.PaymentStatus.CAPTURED) AND o.orderStatus NOT IN :excludedStatuses")
    long countAwaitingPaymentForVendor(
            @Param("vendorId") Long vendorId,
            @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    Page<Order> findByDeliveryPartnerIdOrderByCreatedAtDesc(Long deliveryPartnerId, Pageable pageable);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.createdAt >= :start AND o.createdAt < :end")
    long countByVendorIdAndCreatedAtBetween(
            @Param("vendorId") Long vendorId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.orderStatus IN :statuses")
    long countByVendorIdAndOrderStatusIn(
            @Param("vendorId") Long vendorId,
            @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.vendorId = :vendorId AND o.createdAt >= :start AND o.createdAt < :end AND o.paymentStatus = :paymentStatus AND o.orderStatus NOT IN :excludedStatuses")
    BigDecimal sumTotalByVendorIdAndCreatedAtBetweenAndPaymentStatusAndOrderStatusNotIn(
            @Param("vendorId") Long vendorId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorId = :vendorId AND o.orderStatus = :status AND ((o.deliveredAt >= :start AND o.deliveredAt < :end) OR (o.deliveredAt IS NULL AND o.createdAt >= :start AND o.createdAt < :end))")
    long countCompletedOrdersForVendorToday(
            @Param("vendorId") Long vendorId,
            @Param("status") OrderStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
