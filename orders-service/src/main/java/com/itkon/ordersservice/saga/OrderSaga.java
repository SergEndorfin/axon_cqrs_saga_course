package com.itkon.ordersservice.saga;

import com.itkon.core.commands.CancelProductReservationCommand;
import com.itkon.core.commands.ProcessPaymentCommand;
import com.itkon.core.commands.ReserveProductCommand;
import com.itkon.core.events.PaymentProcessedEvent;
import com.itkon.core.events.ProductReservationCancelledEvent;
import com.itkon.core.events.ProductReservedEvent;
import com.itkon.core.model.User;
import com.itkon.core.query.FetchUserPaymentDetailsQuery;
import com.itkon.ordersservice.command.commands.ApprovedOrderCommand;
import com.itkon.ordersservice.command.commands.RejectOrderCommand;
import com.itkon.ordersservice.core.events.OrderApprovedEvent;
import com.itkon.ordersservice.core.events.OrderCreatedEvent;
import com.itkon.ordersservice.core.events.OrderRejectedEvent;
import com.itkon.ordersservice.core.model.OrderSummary;
import com.itkon.ordersservice.query.FindOrderQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Saga
@Slf4j
public class OrderSaga {

    private static final String PAYMENT_PROCESSING_DEADLINE = "payment-processing-deadline";

    private String scheduleId;

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    @Autowired
    private transient QueryUpdateEmitter queryUpdateEmitter;


    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {
        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .productId(orderCreatedEvent.getProductId())
                .orderId(orderCreatedEvent.getOrderId())
                .userId(orderCreatedEvent.getUserId())
                .quantity(orderCreatedEvent.getQuantity())
                .build();

        log.info("OrderCreatedEvent handled for order id: {} and product id: {}",
                reserveProductCommand.getOrderId(), reserveProductCommand.getProductId());

        commandGateway.send(
                reserveProductCommand,
                (commandMessage, commandResultMessage) -> {
                    if (commandResultMessage.isExceptional()) {
                        // Start compensating Transaction
                        var rejectOrderCommand = new RejectOrderCommand(
                                orderCreatedEvent.getOrderId(), commandResultMessage.exceptionResult().getMessage());
                        commandGateway.send(rejectOrderCommand);
                    }
                }
        );
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservedEvent productReservedEvent) {
        // Process user handler
        log.info("ProductReservedEvent handled for order id: {} and product id: {}",
                productReservedEvent.getOrderId(), productReservedEvent.getProductId());

        FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery =
                new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());

        User userWithPaymentDetails = null;

        try {
            userWithPaymentDetails = queryGateway
                    .query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class))
                    .join();
        } catch (Exception e) {
            log.error(e.getMessage());
            // start compensating transaction.
            cancelProductReservation(productReservedEvent, e.getMessage());
            return;
        }

        if (userWithPaymentDetails == null) {
            // start compensating transaction.
            cancelProductReservation(productReservedEvent, "Could not fetch User Payment details.");
            return;
        }
        log.info("Successfully fetched user payment details for user: {}", userWithPaymentDetails.getFirstName());

        scheduleId = deadlineManager.schedule(Duration.of(120, ChronoUnit.SECONDS),
                PAYMENT_PROCESSING_DEADLINE,
                productReservedEvent);

        ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .paymentId(UUID.randomUUID().toString())
                .paymentDetails(userWithPaymentDetails.getPaymentDetails())
                .build();

        String result;
        try {
            result = commandGateway.sendAndWait(processPaymentCommand);
        } catch (Exception e) {
            log.error(e.getMessage());
            // start compensating transaction.
            cancelProductReservation(productReservedEvent, e.getMessage());
            return;
        }

        if (result == null) {
            log.error("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction.");
            // start compensating transaction.
            cancelProductReservation(productReservedEvent, "Could not process user payment with provided payment details.");
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentProcessedEvent paymentProcessedEvent) {
        cancelPaymentProcessingDeadline();

        // Send an ApprovedOrderCommand
        var approvedOrderCommand = new ApprovedOrderCommand(paymentProcessedEvent.getOrderId());
        commandGateway.send(approvedOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderApprovedEvent orderApprovedEvent) {
        log.info("Order is approved. Order Saga is complete for orderId: {}", orderApprovedEvent.getOrderId());
//        SagaLifecycle.end(); // <- programmatic way without @EndSaga annotation!!
        queryUpdateEmitter.emit(
                FindOrderQuery.class,
                findOrderQuery -> true,
                new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getOrderStatus(), "")
        );
    }


    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReservationCancelledEvent productReservationCancelledEvent) {
        // Create and send a RejectOrderCommand
        var rejectOrderCommand = new RejectOrderCommand(
                productReservationCancelledEvent.getOrderId(), productReservationCancelledEvent.getReason());
        commandGateway.send(rejectOrderCommand);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderRejectedEvent orderRejectedEvent) {
        log.info("Successfully rejected order with id: {}", orderRejectedEvent.getOrderId());
        queryUpdateEmitter.emit(
                FindOrderQuery.class,
                findOrderQuery -> true,
                new OrderSummary(orderRejectedEvent.getOrderId(), orderRejectedEvent.getOrderStatus(), orderRejectedEvent.getReason())
        );
    }


    @DeadlineHandler(deadlineName = PAYMENT_PROCESSING_DEADLINE)
    public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {
        log.info("Payment processing deadline took place. Sending a compensating command to cancel the product reservation.");
        cancelProductReservation(productReservedEvent, "Payment timeout occurred.");
    }


    private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {
        cancelPaymentProcessingDeadline();

        var cancelProductReservationCommand = CancelProductReservationCommand.builder()
                .orderId(productReservedEvent.getOrderId())
                .productId(productReservedEvent.getProductId())
                .quantity(productReservedEvent.getQuantity())
                .reason(reason)
                .build();
        commandGateway.send(cancelProductReservationCommand);
    }

    private void cancelPaymentProcessingDeadline() {
        if (scheduleId != null) {
            deadlineManager.cancelSchedule(PAYMENT_PROCESSING_DEADLINE, scheduleId);
            scheduleId = null;
        }
    }
}
