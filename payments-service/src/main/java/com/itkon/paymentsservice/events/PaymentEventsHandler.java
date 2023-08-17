package com.itkon.paymentsservice.events;

import com.itkon.core.events.PaymentProcessedEvent;
import com.itkon.paymentsservice.data.PaymentEntity;
import com.itkon.paymentsservice.data.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventsHandler {

    private final PaymentRepository paymentRepository;

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("PaymentProcessedEvent is called for orderId: " + event.getOrderId());

        PaymentEntity paymentEntity = new PaymentEntity();
        BeanUtils.copyProperties(event, paymentEntity);

        paymentRepository.save(paymentEntity);
    }
}