package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.domain.OrderItem;
import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import br.com.vpsconsulting.b2b_order_service.exception.InsufficientCreditException;
import br.com.vpsconsulting.b2b_order_service.exception.ResourceNotFoundException;
import br.com.vpsconsulting.b2b_order_service.mapper.OrderMapper;
import br.com.vpsconsulting.b2b_order_service.repository.OrderRepository;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final OrderMapper mapper;

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {

        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com ID: " + dto.getPartnerId()));

        List<OrderItem> domainItems = dto.getItems().stream()
                .map(mapper::toOrderItemEntity)
                .toList();

        Instant now = Instant.now();
        Order order = Order.builder()
                .partnerId(partner.getId())
                .status(OrderStatus.APPROVED)
                .items(domainItems)
                .createdAt(now)
                .updatedAt(now)
                .build();

        order.setTotalAmount(order.calculateTotalAmount());

        long updatedCount = partnerRepository.deductCreditLimit(partner.getId(), order.getTotalAmount());

        if (updatedCount == 0) {
            throw new InsufficientCreditException(
                    String.format("Limite de crédito insuficiente para o parceiro '%s'. Saldo disponível: R$ %.2f, Valor do pedido: R$ %.2f",
                            partner.getName(), partner.getAvailableLimit(), order.getTotalAmount())
            );
        }

        Order savedOrder = orderRepository.save(order);

        return mapper.toResponseDTO(savedOrder);
    }
}