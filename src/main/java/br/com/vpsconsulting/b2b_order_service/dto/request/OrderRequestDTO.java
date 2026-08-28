package br.com.vpsconsulting.b2b_order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotNull(message = "O ID do parceiro é obrigatório")
    private String partnerId;

    @Valid
    @NotEmpty(message = "O pedido deve conter ao menos um item")
    private List<OrderItemRequestDTO> items;
}