package br.com.vpsconsulting.b2b_order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRequestDTO {

    @NotBlank(message = "O nome do parceiro é obrigatório")
    private String name;

    @NotBlank(message = "O CNPJ é obrigatório")
    private String cnpj;

    @NotNull(message = "O limite de crédito é obrigatório")
    @Positive(message = "O limite de crédito deve ser positivo")
    private BigDecimal creditLimit;
}