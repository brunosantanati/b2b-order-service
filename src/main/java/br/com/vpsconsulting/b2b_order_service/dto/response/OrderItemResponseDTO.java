package br.com.vpsconsulting.b2b_order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {

    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}