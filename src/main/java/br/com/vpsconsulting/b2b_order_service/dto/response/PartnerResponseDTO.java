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
public class PartnerResponseDTO {

    private String id;
    private String name;
    private String cnpj;
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
}