package com.itkon.productservice.command;

import com.itkon.core.commands.CancelProductReservationCommand;
import com.itkon.core.commands.ReserveProductCommand;
import com.itkon.core.events.ProductReservationCancelledEvent;
import com.itkon.core.events.ProductReservedEvent;
import com.itkon.productservice.core.events.ProductCreatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

@Aggregate(snapshotTriggerDefinition = "productSnapshotTriggerDefinition")
@NoArgsConstructor
public class ProductAggregate {

    @AggregateIdentifier
    private String productId;
    private String title;
    private BigDecimal price;
    private Integer quantity;

    @CommandHandler
    public ProductAggregate(CreateProductCommand createProductCommand) {

        if (createProductCommand.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price cannot be less or equal that zero.");
        }
        if (createProductCommand.getTitle() == null || createProductCommand.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();

        BeanUtils.copyProperties(createProductCommand, productCreatedEvent);

        AggregateLifecycle.apply(productCreatedEvent);
    }

    @CommandHandler
    public void handle(ReserveProductCommand reserveProductCommand) {
        if (this.quantity < reserveProductCommand.getQuantity()) {
            throw new IllegalArgumentException("Insufficient number of items in stock.");
        }

        ProductReservedEvent reserveProductEvent = ProductReservedEvent.builder()
                .productId(reserveProductCommand.getProductId())
                .orderId(reserveProductCommand.getOrderId())
                .userId(reserveProductCommand.getUserId())
                .quantity(reserveProductCommand.getQuantity())
                .build();

        AggregateLifecycle.apply(reserveProductEvent);
    }

    @CommandHandler
    public void handle(CancelProductReservationCommand cancelProductReservationCommand) {
        var productReservationCancelledEvent = ProductReservationCancelledEvent.builder()
                .orderId(cancelProductReservationCommand.getOrderId())
                .productId(cancelProductReservationCommand.getProductId())
                .userId(cancelProductReservationCommand.getUserId())
                .quantity(cancelProductReservationCommand.getQuantity())
                .reason(cancelProductReservationCommand.getReason())
                .build();

        AggregateLifecycle.apply(productReservationCancelledEvent);
    }

    @EventSourcingHandler
    public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
        this.quantity += productReservationCancelledEvent.getQuantity();
    }


    @EventSourcingHandler
    public void on(ProductCreatedEvent productCreatedEvent) {
        this.productId = productCreatedEvent.getProductId();
        this.price = productCreatedEvent.getPrice();
        this.title = productCreatedEvent.getTitle();
        this.quantity = productCreatedEvent.getQuantity();
    }

    @EventSourcingHandler
    public void on(ProductReservedEvent reserveProductEvent) {
        this.quantity -= reserveProductEvent.getQuantity();
    }
}
