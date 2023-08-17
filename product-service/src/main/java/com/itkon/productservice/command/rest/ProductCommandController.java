package com.itkon.productservice.command.rest;

import com.itkon.productservice.command.CreateProductCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("products")
@RequiredArgsConstructor
public class ProductCommandController {

    private final Environment environment;
    private final CommandGateway commandGateway;

    @PostMapping
    public String createProduct(@Valid @RequestBody CreateProductRestModel createProductRestModel) {
        CreateProductCommand createProductCommand = CreateProductCommand.builder()
                .price(createProductRestModel.getPrice())
                .title(createProductRestModel.getTitle())
                .quantity(createProductRestModel.getQuantity())
                .productId(UUID.randomUUID().toString())
                .build();

        String returnValue;
//        try {
            returnValue = commandGateway.sendAndWait(createProductCommand);
//        } catch (Exception e) {
//            returnValue = e.getLocalizedMessage();
//        }

        return returnValue;
    }

//    @GetMapping
//    public String getProduct() {
//        return "test getProduct: " + environment.getProperty("local.server.port");
//    }
//
//    @PutMapping
//    public String updateProduct() {
//        return "test updateProduct";
//    }
//
//    @DeleteMapping
//    public String deleteProduct() {
//        return "deleteProduct";
//    }
}
