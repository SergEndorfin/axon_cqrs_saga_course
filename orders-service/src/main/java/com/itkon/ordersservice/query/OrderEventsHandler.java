package com.itkon.ordersservice.query;

import com.itkon.ordersservice.core.data.OrderEntity;
import com.itkon.ordersservice.core.events.OrderApprovedEvent;
import com.itkon.ordersservice.core.events.OrderCreatedEvent;
import com.itkon.ordersservice.core.events.OrderRejectedEvent;
import com.itkon.ordersservice.core.repositories.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("order-group")
@RequiredArgsConstructor
public class OrderEventsHandler {

    private final OrdersRepository ordersRepository;

    @EventHandler
    public void on(OrderCreatedEvent event) {
        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(event, orderEntity);
        this.ordersRepository.save(orderEntity);
    }

    @EventHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
        OrderEntity orderEntity = this.ordersRepository.findByOrderId(orderApprovedEvent.getOrderId());
        if (orderEntity == null) {
            // Todo: do something
            return;
        }
        orderEntity.setOrderStatus(orderApprovedEvent.getOrderStatus());
        this.ordersRepository.save(orderEntity);
    }

    @EventHandler
    public void on(OrderRejectedEvent orderRejectedEvent) {
        OrderEntity orderEntity = this.ordersRepository.findByOrderId(orderRejectedEvent.getOrderId());
        orderEntity.setOrderStatus(orderRejectedEvent.getOrderStatus());
        this.ordersRepository.save(orderEntity);
    }
}