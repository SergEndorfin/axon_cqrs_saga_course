package com.itkon.ordersservice.core.data;


import com.itkon.ordersservice.core.model.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "orders")
public class OrderEntity implements Serializable {

    private static final long serialVersionUID = 5313493413859894403L;

    @Id
    @Column(unique = true)
    public String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private String addressId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
}