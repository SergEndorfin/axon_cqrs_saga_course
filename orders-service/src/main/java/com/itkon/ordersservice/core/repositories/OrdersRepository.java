package com.itkon.ordersservice.core.repositories;

import com.itkon.ordersservice.core.data.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<OrderEntity, String> {

    OrderEntity findByOrderId(String orderId);
}