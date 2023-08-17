package com.itkon.productservice;

import com.itkon.productservice.command.interceptors.CreateProductCommandInterceptor;
import com.itkon.core.config.AxonXStreamConfig;
import com.itkon.productservice.core.errorhandling.ProductServiceEventsErrorHandler;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import({AxonXStreamConfig.class})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    @Autowired
    public void registerCreateProductCommandInterceptor(ApplicationContext context, CommandBus commandBus) {
        commandBus.registerDispatchInterceptor(
                context.getBean(CreateProductCommandInterceptor.class)
        );
    }

    @Autowired
    public void configure(EventProcessingConfigurer config) {
        config.registerListenerInvocationErrorHandler("product-group",
                configuration -> new ProductServiceEventsErrorHandler());
    }

    @Bean(name = "productSnapshotTriggerDefinition")
    public SnapshotTriggerDefinition productSnapshotTriggerDefinition(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 3);
    }
}
