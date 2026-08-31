package br.com.vpsconsulting.b2b_order_service.dto.request;

import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDTO {

    @NotNull(message = "O novo status do pedido é obrigatório")
    private OrderStatus status;
}