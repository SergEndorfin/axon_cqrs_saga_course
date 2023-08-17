package com.itkon.productservice.command;

import com.itkon.productservice.core.data.ProductLookupEntity;
import com.itkon.productservice.core.data.ProductLookupRepository;
import com.itkon.productservice.core.events.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("product-group")
@RequiredArgsConstructor
public class ProductLookupEventsHandler {

    private final ProductLookupRepository productLookupRepository;

    @EventHandler
    public void on(ProductCreatedEvent event) {
        var productLookupEntity = new ProductLookupEntity(event.getProductId(), event.getTitle());
        productLookupRepository.save(productLookupEntity);
    }

    @ResetHandler
    public void reset() {
        productLookupRepository.deleteAll();
    }
}
