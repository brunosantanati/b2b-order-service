package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.PartnerResponseDTO;
import br.com.vpsconsulting.b2b_order_service.exception.ResourceNotFoundException;
import br.com.vpsconsulting.b2b_order_service.mapper.PartnerMapper;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    @Transactional
    public PartnerResponseDTO createPartner(PartnerRequestDTO request) {
        if (partnerRepository.existsByCnpj(request.getCnpj())) {
            throw new IllegalArgumentException("Já existe um parceiro cadastrado com o CNPJ: " + request.getCnpj());
        }

        Partner partner = partnerMapper.toEntity(request);

        Partner savedPartner = partnerRepository.save(partner);
        log.info("Parceiro cadastrado com sucesso. ID: {}, CNPJ: {}, Limite: {}",
                savedPartner.getId(), savedPartner.getCnpj(), savedPartner.getCreditLimit());

        return partnerMapper.toResponseDTO(savedPartner);
    }

    public PartnerResponseDTO findById(String id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com o ID: " + id));
        return partnerMapper.toResponseDTO(partner);
    }

    public List<PartnerResponseDTO> findAll() {
        return partnerRepository.findAll()
                .stream()
                .map(partnerMapper::toResponseDTO)
                .toList();
    }
}