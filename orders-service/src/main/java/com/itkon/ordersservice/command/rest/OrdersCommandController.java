package com.itkon.ordersservice.command.rest;

import com.itkon.ordersservice.command.commands.CreateOrderCommand;
import com.itkon.ordersservice.core.model.OrderStatus;
import com.itkon.ordersservice.core.model.OrderSummary;
import com.itkon.ordersservice.query.FindOrderQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrdersCommandController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @PostMapping
    public OrderSummary createOrder(@Valid @RequestBody OrderCreateRest order) {

        String userId = "27b95829-4f3f-4ddf-8983-151ba010e35b";
        String orderId = UUID.randomUUID().toString();

        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .addressId(order.getAddressId())
                .productId(order.getProductId())
                .userId(userId)
                .quantity(order.getQuantity())
                .orderId(orderId)
                .orderStatus(OrderStatus.CREATED)
                .build();

        try (SubscriptionQueryResult<OrderSummary, OrderSummary> subscriptionQuery =
                     queryGateway.subscriptionQuery(
                             new FindOrderQuery(orderId),
                             ResponseTypes.instanceOf(OrderSummary.class),
                             ResponseTypes.instanceOf(OrderSummary.class)
                     )
        ) {
            commandGateway.sendAndWait(createOrderCommand);
            return subscriptionQuery.updates().blockFirst();
        }
    }
}
