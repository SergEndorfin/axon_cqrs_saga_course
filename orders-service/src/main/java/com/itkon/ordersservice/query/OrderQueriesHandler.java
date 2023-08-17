package com.itkon.ordersservice.query;

import com.itkon.ordersservice.core.model.OrderSummary;
import com.itkon.ordersservice.core.repositories.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderQueriesHandler {

    private final OrdersRepository ordersRepository;

    @QueryHandler
    public OrderSummary findOne(FindOrderQuery findOrderQuery) {
        var orderEntity = ordersRepository.findByOrderId(findOrderQuery.getOrderId());
        return new OrderSummary(orderEntity.orderId, orderEntity.getOrderStatus(), "");
    }
}
