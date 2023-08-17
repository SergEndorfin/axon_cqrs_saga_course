package com.itkon.productservice.query;

import com.itkon.core.events.ProductReservationCancelledEvent;
import com.itkon.core.events.ProductReservedEvent;
import com.itkon.productservice.core.data.ProductEntity;
import com.itkon.productservice.core.events.ProductCreatedEvent;
import com.itkon.productservice.core.data.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("product-group")
@RequiredArgsConstructor
@Slf4j
public class ProductEventsHandler {

    private final ProductRepository productRepository;

    @EventHandler
    public void on(ProductCreatedEvent event) throws Exception {
        ProductEntity productEntity = new ProductEntity();
        BeanUtils.copyProperties(event, productEntity);

        try {
            productRepository.save(productEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void on(ProductReservedEvent productReservedEvent) {
        ProductEntity productEntity = productRepository.findByProductId(productReservedEvent.getProductId());

        log.debug("ProductReservedEvent: current product quantity: {}", productEntity.getQuantity());

        productEntity.setQuantity(productEntity.getQuantity() - productReservedEvent.getQuantity());
        productRepository.save(productEntity);

        log.debug("ProductReservedEvent: NEW product quantity: {}", productEntity.getQuantity());
        log.info("ProductReservedEvent is called for order id: {} and product id: {}",
                productReservedEvent.getOrderId(), productReservedEvent.getProductId());
    }

    @EventHandler
    public void on(ProductReservationCancelledEvent productReservationCancelledEvent) {
        ProductEntity productEntity = productRepository.findByProductId(productReservationCancelledEvent.getProductId());

        log.debug("ProductReservationCancelledEvent: current product quantity: {}", productReservationCancelledEvent.getQuantity());

        productEntity.setQuantity(productEntity.getQuantity() + productReservationCancelledEvent.getQuantity());
        productRepository.save(productEntity);

        log.debug("ProductReservationCancelledEvent: NEW product quantity: {}", productReservationCancelledEvent.getQuantity());
    }


    @ExceptionHandler(resultType = IllegalArgumentException.class)
    public void handle(IllegalArgumentException exception) {
        //  log...
    }

    @ExceptionHandler(resultType = Exception.class)
    public void handle(Exception exception) throws Exception {
        //  log...
        throw exception;
    }


    @ResetHandler
    public void reset() {
        this.productRepository.deleteAll();
    }
}
