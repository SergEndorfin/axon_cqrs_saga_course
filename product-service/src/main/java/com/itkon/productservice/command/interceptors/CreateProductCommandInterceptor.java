package com.itkon.productservice.command.interceptors;

import com.itkon.productservice.command.CreateProductCommand;
import com.itkon.productservice.core.data.ProductLookupEntity;
import com.itkon.productservice.core.data.ProductLookupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;

@Component
@Slf4j
@RequiredArgsConstructor
public class CreateProductCommandInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    private final ProductLookupRepository productLookupRepository;

    @Nonnull
    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(@Nonnull List<? extends CommandMessage<?>> list) {

        return (index, command) -> {
            log.info("Intercepted command: {}", command.getPayloadType());

            if (CreateProductCommand.class.equals(command.getPayloadType())) {
                CreateProductCommand createProductCommand = (CreateProductCommand) command.getPayload();

                ProductLookupEntity productLookupEntity = productLookupRepository.findByProductIdOrTitle(
                        createProductCommand.getProductId(), createProductCommand.getTitle());

                if (productLookupEntity != null) {
                    throw new IllegalStateException(String.format("Product with id: %s or title %s already exist.",
                            productLookupEntity.getProductId(), productLookupEntity.getTitle())
                    );
                }
            }

            return command;
        };
    }
}
