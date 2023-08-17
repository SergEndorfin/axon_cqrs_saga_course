package com.itkon.productservice.core.errorhandling;

import java.util.Date;

public record ErrorMessage(Date timestamp, String message) {
}
