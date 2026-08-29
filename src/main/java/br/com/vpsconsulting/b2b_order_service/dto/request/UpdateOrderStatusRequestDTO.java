package br.com.vpsconsulting.b2b_order_service.dto.request;

import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequestDTO {

    @NotNull(message = "O novo status do pedido é obrigatório")
    private OrderStatus status;
}