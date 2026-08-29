package br.com.vpsconsulting.b2b_order_service.dto.event;

import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {

    private String orderId;
    private String partnerId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private Instant updatedAt;
}