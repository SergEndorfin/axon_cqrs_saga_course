package com.itkon.ordersservice.command;

import com.itkon.ordersservice.command.commands.ApprovedOrderCommand;
import com.itkon.ordersservice.command.commands.CreateOrderCommand;
import com.itkon.ordersservice.command.commands.RejectOrderCommand;
import com.itkon.ordersservice.core.events.OrderApprovedEvent;
import com.itkon.ordersservice.core.events.OrderCreatedEvent;
import com.itkon.ordersservice.core.events.OrderRejectedEvent;
import com.itkon.ordersservice.core.model.OrderStatus;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Aggregate
@NoArgsConstructor
public class OrderAggregate {

    @AggregateIdentifier
    private String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private String addressId;
    private OrderStatus orderStatus;

    @CommandHandler
    public OrderAggregate(CreateOrderCommand createOrderCommand) {
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
        BeanUtils.copyProperties(createOrderCommand, orderCreatedEvent);

        AggregateLifecycle.apply(orderCreatedEvent);
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent orderCreatedEvent) {
        this.orderId = orderCreatedEvent.getOrderId();
        this.productId = orderCreatedEvent.getProductId();
        this.userId = orderCreatedEvent.getUserId();
        this.addressId = orderCreatedEvent.getAddressId();
        this.quantity = orderCreatedEvent.getQuantity();
        this.orderStatus = orderCreatedEvent.getOrderStatus();
    }

    @CommandHandler
    public void handle(ApprovedOrderCommand approvedOrderCommand) {
        // Create and publish OrderApprovedEvent
        var orderApprovedEvent = new OrderApprovedEvent(approvedOrderCommand.getOrderId());

        AggregateLifecycle.apply(orderApprovedEvent);
    }

    @EventSourcingHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
        this.orderStatus = orderApprovedEvent.getOrderStatus();
    }

    @CommandHandler
    public void handle(RejectOrderCommand rejectOrderCommand) {
        var orderApprovedEvent = new OrderRejectedEvent(rejectOrderCommand.getOrderId(), rejectOrderCommand.getReason());
        AggregateLifecycle.apply(orderApprovedEvent);
    }

    @EventSourcingHandler
    public void on(OrderRejectedEvent orderRejectedEvent) {
        this.orderStatus = orderRejectedEvent.getOrderStatus();
    }
}

