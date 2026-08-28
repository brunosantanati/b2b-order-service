package br.com.vpsconsulting.b2b_order_service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "partners")
public class Partner {

    @Id
    private String id;
    private String name;
    private String cnpj;
    private BigDecimal creditLimit;
    private BigDecimal availableLimit;
}