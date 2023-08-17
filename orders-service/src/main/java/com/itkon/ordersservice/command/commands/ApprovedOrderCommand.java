package com.itkon.ordersservice.command.commands;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class ApprovedOrderCommand {
    @TargetAggregateIdentifier
    private final String orderId;
}
