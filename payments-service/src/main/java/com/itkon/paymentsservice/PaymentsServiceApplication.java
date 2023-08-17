package com.itkon.paymentsservice;

import com.itkon.core.config.AxonXStreamConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({AxonXStreamConfig.class})
public class PaymentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsServiceApplication.class, args);
    }

}