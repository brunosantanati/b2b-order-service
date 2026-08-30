package br.com.vpsconsulting.b2b_order_service.mapper;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.PartnerResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PartnerMapper {

    public PartnerResponseDTO toResponseDTO(Partner partner) {
        if (partner == null) {
            return null;
        }

        return PartnerResponseDTO.builder()
                .id(partner.getId())
                .name(partner.getName())
                .cnpj(partner.getCnpj())
                .creditLimit(partner.getCreditLimit())
                .availableLimit(partner.getAvailableLimit())
                .build();
    }

    public Partner toEntity(PartnerRequestDTO request) {
        if (request == null) {
            return null;
        }

        return Partner.builder()
                .name(request.getName())
                .cnpj(request.getCnpj())
                .creditLimit(request.getCreditLimit())
                .availableLimit(request.getCreditLimit())
                .build();
    }

}
