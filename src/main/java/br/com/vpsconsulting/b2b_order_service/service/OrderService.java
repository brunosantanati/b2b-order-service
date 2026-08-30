package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.domain.OrderItem;
import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.event.OrderStatusUpdatedEvent;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderFilterRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.UpdateOrderStatusRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import br.com.vpsconsulting.b2b_order_service.exception.InsufficientCreditException;
import br.com.vpsconsulting.b2b_order_service.exception.ResourceNotFoundException;
import br.com.vpsconsulting.b2b_order_service.mapper.OrderMapper;
import br.com.vpsconsulting.b2b_order_service.repository.OrderRepository;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final MongoTemplate mongoTemplate;
    private final OrderMapper orderMapper;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {

        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Parceiro não encontrado com ID: " + dto.getPartnerId()));

        List<OrderItem> domainItems = dto.getItems().stream()
                .map(orderMapper::toOrderItemEntity)
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

        return orderMapper.toResponseDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        return orderMapper.toResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findAllByFilter(OrderFilterRequestDTO filter) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(filter.getOrderId())) {
            criteriaList.add(Criteria.where("id").is(filter.getOrderId()));
        }

        if (StringUtils.hasText(filter.getPartnerId())) {
            criteriaList.add(Criteria.where("partnerId").is(filter.getPartnerId()));
        }

        if (filter.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(filter.getStatus()));
        }

        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(filter.getStartDate()).lte(filter.getEndDate()));
        } else if (filter.getStartDate() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(filter.getStartDate()));
        } else if (filter.getEndDate() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(filter.getEndDate()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<Order> orders = mongoTemplate.find(query, Order.class);

        return orders.stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public OrderResponseDTO cancelOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));

        order.cancel();

        long updatedCount = partnerRepository.refundCreditLimit(order.getPartnerId(), order.getTotalAmount());

        if (updatedCount == 0) {
            throw new ResourceNotFoundException("Parceiro não encontrado para estorno de limite. ID: " + order.getPartnerId());
        }

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponseDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(String id, UpdateOrderStatusRequestDTO request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));

        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        if (previousStatus == newStatus) {
            log.info("O pedido {} já se encontra no status {}", id, newStatus);
            return orderMapper.toResponseDTO(order);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                .orderId(updatedOrder.getId())
                .partnerId(updatedOrder.getPartnerId())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .updatedAt(updatedOrder.getUpdatedAt())
                .build();

        kafkaProducerService.sendOrderStatusUpdatedEvent(event);

        return orderMapper.toResponseDTO(updatedOrder);
    }
}