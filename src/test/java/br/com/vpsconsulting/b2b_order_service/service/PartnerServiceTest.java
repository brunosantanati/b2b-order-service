package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.PartnerResponseDTO;
import br.com.vpsconsulting.b2b_order_service.exception.CnpjAlreadyExistsException;
import br.com.vpsconsulting.b2b_order_service.exception.ResourceNotFoundException;
import br.com.vpsconsulting.b2b_order_service.mapper.PartnerMapper;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private PartnerService partnerService;

    @Nested
    @DisplayName("Testes do método createPartner")
    class CreatePartnerTests {

        @Test
        @DisplayName("Deve criar um parceiro com sucesso quando o CNPJ não existir")
        void shouldCreatePartnerSuccessfully() {
            // Arrange
            var partnerId = "66d1f8b2e4b0123456789abc";
            var name = "Empresa Teste LTDA";
            var cnpj = "00000000000191";
            var creditLimit = new BigDecimal("200000.00");

            var request = PartnerRequestDTO.builder()
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .build();

            var partnerEntityToSave = Partner.builder()
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(creditLimit)
                    .build();

            var savedPartner = Partner.builder()
                    .id(partnerId)
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(creditLimit)
                    .build();

            var expectedResponse = PartnerResponseDTO.builder()
                    .id(partnerId)
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(creditLimit)
                    .build();

            when(partnerRepository.existsByCnpj(cnpj)).thenReturn(false);
            when(partnerMapper.toEntity(request)).thenReturn(partnerEntityToSave);
            when(partnerRepository.save(partnerEntityToSave)).thenReturn(savedPartner);
            when(partnerMapper.toResponseDTO(savedPartner)).thenReturn(expectedResponse);

            // Act
            PartnerResponseDTO result = partnerService.createPartner(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(partnerId);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getCnpj()).isEqualTo(cnpj);
            assertThat(result.getCreditLimit()).isEqualByComparingTo(creditLimit);
            assertThat(result.getAvailableLimit()).isEqualByComparingTo(creditLimit);

            verify(partnerRepository).existsByCnpj(cnpj);
            verify(partnerMapper).toEntity(request);
            verify(partnerRepository).save(partnerEntityToSave);
            verify(partnerMapper).toResponseDTO(savedPartner);
        }

        @Test
        @DisplayName("Deve lançar CnpjAlreadyExistsException quando o CNPJ já estiver cadastrado")
        void shouldThrowExceptionWhenCnpjAlreadyExists() {
            // Arrange
            var cnpj = "00000000000191";
            var request = PartnerRequestDTO.builder()
                    .name("Empresa Teste LTDA")
                    .cnpj(cnpj)
                    .creditLimit(new BigDecimal("200000.00"))
                    .build();

            when(partnerRepository.existsByCnpj(cnpj)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> partnerService.createPartner(request))
                    .isInstanceOf(CnpjAlreadyExistsException.class)
                    .hasMessage("Já existe um parceiro cadastrado com o CNPJ: " + cnpj);

            verify(partnerRepository).existsByCnpj(cnpj);
            verify(partnerMapper, never()).toEntity(any());
            verify(partnerRepository, never()).save(any());
            verify(partnerMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    @DisplayName("Testes do método findById")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar o parceiro quando o ID for encontrado")
        void shouldReturnPartnerWhenIdExists() {
            // Arrange
            var partnerId = "66d1f8b2e4b0123456789abc";
            var name = "Empresa Teste LTDA";
            var cnpj = "00000000000191";
            var creditLimit = new BigDecimal("200000.00");
            var availableLimit = new BigDecimal("150000.00");

            var partner = Partner.builder()
                    .id(partnerId)
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(availableLimit)
                    .build();

            var expectedResponse = PartnerResponseDTO.builder()
                    .id(partnerId)
                    .name(name)
                    .cnpj(cnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(availableLimit)
                    .build();

            when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
            when(partnerMapper.toResponseDTO(partner)).thenReturn(expectedResponse);

            // Act
            PartnerResponseDTO result = partnerService.findById(partnerId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(partnerId);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getCnpj()).isEqualTo(cnpj);
            assertThat(result.getCreditLimit()).isEqualByComparingTo(creditLimit);
            assertThat(result.getAvailableLimit()).isEqualByComparingTo(availableLimit);

            verify(partnerRepository).findById(partnerId);
            verify(partnerMapper).toResponseDTO(partner);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o ID não for encontrado")
        void shouldThrowExceptionWhenIdDoesNotExist() {
            // Arrange
            var invalidId = "invalid-id";
            when(partnerRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> partnerService.findById(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Parceiro não encontrado com o ID: " + invalidId);

            verify(partnerRepository).findById(invalidId);
            verify(partnerMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    @DisplayName("Testes do método findAll")
    class FindAllTests {

        @Test
        @DisplayName("Deve retornar a lista de parceiros cadastrados")
        void shouldReturnListOfPartners() {
            // Arrange
            var id1 = "1";
            var name1 = "Parceiro Um LTDA";
            var cnpj1 = "00000000000191";
            var creditLimit1 = new BigDecimal("100000.00");

            var id2 = "2";
            var name2 = "Parceiro Dois LTDA";
            var cnpj2 = "11255445500001";
            var creditLimit2 = new BigDecimal("300000.00");
            var availableLimit2 = new BigDecimal("250000.00");

            var partner1 = Partner.builder()
                    .id(id1)
                    .name(name1)
                    .cnpj(cnpj1)
                    .creditLimit(creditLimit1)
                    .availableLimit(creditLimit1)
                    .build();

            var partner2 = Partner.builder()
                    .id(id2)
                    .name(name2)
                    .cnpj(cnpj2)
                    .creditLimit(creditLimit2)
                    .availableLimit(availableLimit2)
                    .build();

            var response1 = PartnerResponseDTO.builder()
                    .id(id1)
                    .name(name1)
                    .cnpj(cnpj1)
                    .creditLimit(creditLimit1)
                    .availableLimit(creditLimit1)
                    .build();

            var response2 = PartnerResponseDTO.builder()
                    .id(id2)
                    .name(name2)
                    .cnpj(cnpj2)
                    .creditLimit(creditLimit2)
                    .availableLimit(availableLimit2)
                    .build();

            when(partnerRepository.findAll()).thenReturn(List.of(partner1, partner2));
            when(partnerMapper.toResponseDTO(partner1)).thenReturn(response1);
            when(partnerMapper.toResponseDTO(partner2)).thenReturn(response2);

            // Act
            List<PartnerResponseDTO> result = partnerService.findAll();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(PartnerResponseDTO::getId).containsExactly(id1, id2);
            assertThat(result).extracting(PartnerResponseDTO::getAvailableLimit)
                    .containsExactly(creditLimit1, availableLimit2);

            verify(partnerRepository).findAll();
            verify(partnerMapper, times(2)).toResponseDTO(any());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver parceiros cadastrados")
        void shouldReturnEmptyListWhenNoPartnersExist() {
            // Arrange
            when(partnerRepository.findAll()).thenReturn(List.of());

            // Act
            List<PartnerResponseDTO> result = partnerService.findAll();

            // Assert
            assertThat(result).isEmpty();

            verify(partnerRepository).findAll();
            verify(partnerMapper, never()).toResponseDTO(any());
        }
    }
}