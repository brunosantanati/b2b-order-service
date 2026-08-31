package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.domain.OrderItem;
import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import br.com.vpsconsulting.b2b_order_service.dto.event.OrderStatusUpdatedEvent;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderFilterRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderItemRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.UpdateOrderStatusRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderItemResponseDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import br.com.vpsconsulting.b2b_order_service.exception.InsufficientCreditException;
import br.com.vpsconsulting.b2b_order_service.exception.ResourceNotFoundException;
import br.com.vpsconsulting.b2b_order_service.mapper.OrderMapper;
import br.com.vpsconsulting.b2b_order_service.repository.OrderRepository;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderStatusUpdatedEvent> eventCaptor;

    @Nested
    @DisplayName("Testes do método createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Deve criar um pedido com sucesso, debitar limite e publicar evento no Kafka")
        void shouldCreateOrderSuccessfully() {
            // Arrange
            var partnerId = "66d1f8b2e4b0123456789abc";
            var orderId = "77e2f9c3f5c123456789def";
            var productId = "PROD-001";
            var partnerName = "Empresa Compradora LTDA";
            var partnerCnpj = "00000000000191";
            var creditLimit = new BigDecimal("200000.00");
            var availableLimit = new BigDecimal("200000.00");
            var unitPrice = new BigDecimal("150.00");
            var quantity = 10;
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var itemRequest = OrderItemRequestDTO.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            var orderRequest = OrderRequestDTO.builder()
                    .partnerId(partnerId)
                    .items(List.of(itemRequest))
                    .build();

            var partner = Partner.builder()
                    .id(partnerId)
                    .name(partnerName)
                    .cnpj(partnerCnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(availableLimit)
                    .build();

            var orderItemDomain = OrderItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            var savedOrder = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .items(List.of(orderItemDomain))
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var itemResponse = OrderItemResponseDTO.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subTotal(totalAmount)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .items(List.of(itemResponse))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
            when(orderMapper.toOrderItemEntity(itemRequest)).thenReturn(orderItemDomain);
            when(partnerRepository.deductCreditLimit(eq(partnerId), any(BigDecimal.class))).thenReturn(1L);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderMapper.toResponseDTO(savedOrder)).thenReturn(expectedResponse);

            // Act
            OrderResponseDTO result = orderService.createOrder(orderRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getPartnerId()).isEqualTo(partnerId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(totalAmount);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getSubTotal()).isEqualByComparingTo(totalAmount);

            verify(partnerRepository).findById(partnerId);
            verify(orderMapper).toOrderItemEntity(itemRequest);
            verify(partnerRepository).deductCreditLimit(eq(partnerId), eq(totalAmount));
            verify(orderRepository).save(orderCaptor.capture());
            verify(orderMapper).toResponseDTO(savedOrder);

            Order capturedOrder = orderCaptor.getValue();
            assertThat(capturedOrder.getPartnerId()).isEqualTo(partnerId);
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getTotalAmount()).isEqualByComparingTo(totalAmount);

            verify(kafkaProducerService).publishOrderStatusUpdatedEvent(eventCaptor.capture());
            OrderStatusUpdatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(publishedEvent.getPartnerId()).isEqualTo(partnerId);
            assertThat(publishedEvent.getPreviousStatus()).isNull();
            assertThat(publishedEvent.getNewStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o parceiro não for encontrado")
        void shouldThrowExceptionWhenPartnerNotFound() {
            // Arrange
            var invalidPartnerId = "invalid-partner-id";
            var orderRequest = OrderRequestDTO.builder()
                    .partnerId(invalidPartnerId)
                    .items(List.of())
                    .build();

            when(partnerRepository.findById(invalidPartnerId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Parceiro não encontrado com ID: " + invalidPartnerId);

            verify(partnerRepository).findById(invalidPartnerId);
            verify(orderMapper, never()).toOrderItemEntity(any());
            verify(partnerRepository, never()).deductCreditLimit(any(), any());
            verify(orderRepository, never()).save(any());
            verify(orderMapper, never()).toResponseDTO(any());
            verify(kafkaProducerService, never()).publishOrderStatusUpdatedEvent(any());
        }

        @Test
        @DisplayName("Deve lançar InsufficientCreditException quando o limite retornado for 0")
        void shouldThrowExceptionWhenInsufficientCredit() {
            // Arrange
            var partnerId = "66d1f8b2e4b0123456789abc";
            var productId = "PROD-001";
            var partnerName = "Empresa Compradora LTDA";
            var partnerCnpj = "00000000000191";
            var creditLimit = new BigDecimal("200000.00");
            var availableLimit = new BigDecimal("200000.00");
            var unitPrice = new BigDecimal("150000.00");
            var quantity = 2;
            var totalAmount = new BigDecimal("300000.00");

            var itemRequest = OrderItemRequestDTO.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            var orderRequest = OrderRequestDTO.builder()
                    .partnerId(partnerId)
                    .items(List.of(itemRequest))
                    .build();

            var partner = Partner.builder()
                    .id(partnerId)
                    .name(partnerName)
                    .cnpj(partnerCnpj)
                    .creditLimit(creditLimit)
                    .availableLimit(availableLimit)
                    .build();

            var orderItemDomain = OrderItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
            when(orderMapper.toOrderItemEntity(itemRequest)).thenReturn(orderItemDomain);
            when(partnerRepository.deductCreditLimit(eq(partnerId), any(BigDecimal.class))).thenReturn(0L);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                    .isInstanceOf(InsufficientCreditException.class)
                    .hasMessageContaining("Limite de crédito insuficiente para o parceiro '" + partnerName + "'");

            verify(partnerRepository).findById(partnerId);
            verify(orderMapper).toOrderItemEntity(itemRequest);
            verify(partnerRepository).deductCreditLimit(partnerId, totalAmount);
            verify(orderRepository, never()).save(any());
            verify(orderMapper, never()).toResponseDTO(any());
            verify(kafkaProducerService, never()).publishOrderStatusUpdatedEvent(any());
        }
    }

    @Nested
    @DisplayName("Testes do método findById")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar o pedido quando o ID for encontrado")
        void shouldReturnOrderWhenIdExists() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var order = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toResponseDTO(order)).thenReturn(expectedResponse);

            // Act
            OrderResponseDTO result = orderService.findById(orderId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getPartnerId()).isEqualTo(partnerId);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(totalAmount);

            verify(orderRepository).findById(orderId);
            verify(orderMapper).toResponseDTO(order);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException quando o ID do pedido não existir")
        void shouldThrowExceptionWhenOrderIdDoesNotExist() {
            // Arrange
            var invalidOrderId = "invalid-order-id";
            when(orderRepository.findById(invalidOrderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.findById(invalidOrderId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Pedido não encontrado com ID: " + invalidOrderId);

            verify(orderRepository).findById(invalidOrderId);
            verify(orderMapper, never()).toResponseDTO(any()); // Já estava presente
        }
    }

    @Nested
    @DisplayName("Testes do método findAllByFilter")
    class FindAllByFilterTests {

        @Test
        @DisplayName("Deve buscar pedidos aplicando filtros")
        void shouldFindOrdersByFilterSuccessfully() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var filter = OrderFilterRequestDTO.builder()
                    .orderId(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .startDate(now.minusSeconds(86400))
                    .endDate(now)
                    .build();

            var order = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(mongoTemplate.find(any(Query.class), eq(Order.class))).thenReturn(List.of(order));
            when(orderMapper.toResponseDTO(order)).thenReturn(expectedResponse);

            // Act
            List<OrderResponseDTO> result = orderService.findAllByFilter(filter);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(orderId);
            assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(totalAmount);

            verify(mongoTemplate).find(any(Query.class), eq(Order.class));
            verify(orderMapper).toResponseDTO(order);
        }

        @Test
        @DisplayName("Deve buscar todos os pedidos do banco quando nenhum filtro for informado no DTO")
        void shouldFindAllOrdersWhenFilterIsEmpty() {
            // Arrange
            var orderId1 = "77e2f9c3f5c123456789def";
            var orderId2 = "88f3a0d4a6d234567890abc";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var filter = OrderFilterRequestDTO.builder().build(); // DTO sem filtros

            var order1 = Order.builder()
                    .id(orderId1)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var order2 = Order.builder()
                    .id(orderId2)
                    .partnerId(partnerId)
                    .status(OrderStatus.APPROVED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var response1 = OrderResponseDTO.builder()
                    .id(orderId1)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var response2 = OrderResponseDTO.builder()
                    .id(orderId2)
                    .partnerId(partnerId)
                    .status(OrderStatus.APPROVED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(mongoTemplate.find(any(Query.class), eq(Order.class))).thenReturn(List.of(order1, order2));
            when(orderMapper.toResponseDTO(order1)).thenReturn(response1);
            when(orderMapper.toResponseDTO(order2)).thenReturn(response2);

            // Act
            List<OrderResponseDTO> result = orderService.findAllByFilter(filter);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(OrderResponseDTO::getId).containsExactly(orderId1, orderId2);
            assertThat(result).extracting(OrderResponseDTO::getStatus).containsExactly(OrderStatus.PENDING, OrderStatus.APPROVED);

            verify(mongoTemplate).find(any(Query.class), eq(Order.class));
            verify(orderMapper, times(2)).toResponseDTO(any());
        }
    }

    @Nested
    @DisplayName("Testes do método cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Deve cancelar pedido com sucesso, estornar limite do parceiro e publicar evento")
        void shouldCancelOrderSuccessfully() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var order = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var cancelledOrder = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.CANCELLED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.CANCELLED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(partnerRepository.refundCreditLimit(partnerId, totalAmount)).thenReturn(1L);
            when(orderRepository.save(order)).thenReturn(cancelledOrder);
            when(orderMapper.toResponseDTO(cancelledOrder)).thenReturn(expectedResponse);

            // Act
            OrderResponseDTO result = orderService.cancelOrder(orderId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            verify(orderRepository).findById(orderId);
            verify(partnerRepository).refundCreditLimit(partnerId, totalAmount);
            verify(orderRepository).save(order);
            verify(orderMapper).toResponseDTO(cancelledOrder);

            verify(kafkaProducerService).publishOrderStatusUpdatedEvent(eventCaptor.capture());
            OrderStatusUpdatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(publishedEvent.getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(publishedEvent.getNewStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException ao tentar estornar parceiro inexistente no cancelamento")
        void shouldThrowExceptionWhenRefundPartnerNotFound() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");

            var order = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(partnerRepository.refundCreditLimit(partnerId, totalAmount)).thenReturn(0L);

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Parceiro não encontrado para estorno de limite. ID: " + partnerId);

            verify(orderRepository).findById(orderId);
            verify(partnerRepository).refundCreditLimit(partnerId, totalAmount);
            verify(orderRepository, never()).save(any());
            verify(orderMapper, never()).toResponseDTO(any());
            verify(kafkaProducerService, never()).publishOrderStatusUpdatedEvent(any());
        }
    }

    @Nested
    @DisplayName("Testes do método updateOrderStatus")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Deve atualizar o status do pedido com sucesso e publicar evento")
        void shouldUpdateOrderStatusSuccessfully() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var partnerId = "66d1f8b2e4b0123456789abc";
            var totalAmount = new BigDecimal("1500.00");
            var now = Instant.now();

            var updateRequest = UpdateOrderStatusRequestDTO.builder()
                    .status(OrderStatus.APPROVED)
                    .build();

            var order = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var updatedOrder = Order.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.APPROVED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .partnerId(partnerId)
                    .status(OrderStatus.APPROVED)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(updatedOrder);
            when(orderMapper.toResponseDTO(updatedOrder)).thenReturn(expectedResponse);

            // Act
            OrderResponseDTO result = orderService.updateOrderStatus(orderId, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.APPROVED);

            verify(orderRepository).findById(orderId);
            verify(orderRepository).save(order);
            verify(orderMapper).toResponseDTO(updatedOrder);

            verify(kafkaProducerService).publishOrderStatusUpdatedEvent(eventCaptor.capture());
            OrderStatusUpdatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(publishedEvent.getPreviousStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(publishedEvent.getNewStatus()).isEqualTo(OrderStatus.APPROVED);
        }

        @Test
        @DisplayName("Deve retornar sem salvar nem publicar evento se o novo status for idêntico ao atual")
        void shouldReturnWithoutSavingIfStatusIsSame() {
            // Arrange
            var orderId = "77e2f9c3f5c123456789def";
            var totalAmount = new BigDecimal("1500.00");
            var updateRequest = UpdateOrderStatusRequestDTO.builder()
                    .status(OrderStatus.PENDING)
                    .build();

            var order = Order.builder()
                    .id(orderId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .build();

            var expectedResponse = OrderResponseDTO.builder()
                    .id(orderId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderMapper.toResponseDTO(order)).thenReturn(expectedResponse);

            // Act
            OrderResponseDTO result = orderService.updateOrderStatus(orderId, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

            verify(orderRepository).findById(orderId);
            verify(orderMapper).toResponseDTO(order);
            verify(orderRepository, never()).save(any());
            verify(kafkaProducerService, never()).publishOrderStatusUpdatedEvent(any());
        }
    }
}