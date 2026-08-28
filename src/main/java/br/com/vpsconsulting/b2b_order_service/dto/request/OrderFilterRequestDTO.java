package br.com.vpsconsulting.b2b_order_service.dto.request;

import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
public class OrderFilterRequestDTO {

    private String orderId;
    private String partnerId;
    private OrderStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant endDate;
}