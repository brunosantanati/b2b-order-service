package br.com.vpsconsulting.b2b_order_service.mapper;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.domain.OrderItem;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderItemRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderItemResponseDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequestDTO dto) {
        List<OrderItem> items = dto.getItems().stream()
                .map(this::toOrderItemEntity)
                .toList();

        return Order.builder()
                .partnerId(dto.getPartnerId())
                .items(items)
                .build();
    }

    public OrderItem toOrderItemEntity(OrderItemRequestDTO dto) {
        return OrderItem.builder()
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .build();
    }

    public OrderResponseDTO toResponseDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(this::toOrderItemResponseDTO)
                .toList();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .partnerId(order.getPartnerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}