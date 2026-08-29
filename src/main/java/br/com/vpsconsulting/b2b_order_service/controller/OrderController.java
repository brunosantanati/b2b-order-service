package br.com.vpsconsulting.b2b_order_service.controller;

import br.com.vpsconsulting.b2b_order_service.dto.request.OrderFilterRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.UpdateOrderStatusRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import br.com.vpsconsulting.b2b_order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints para gerenciamento e processamento de pedidos B2B")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Criar um novo pedido", description = "Cadastra um novo pedido no sistema B2B e realiza a vinculação com o parceiro comercial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou regras de validação violadas", content = @Content),
            @ApiResponse(responseCode = "404", description = "Parceiro (Partner) não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO request) {
        OrderResponseDTO createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @Operation(summary = "Buscar pedido por ID", description = "Recupera os detalhes completos de um pedido específico a partir do seu ID do MongoDB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> findById(
            @Parameter(description = "ID único do pedido no MongoDB", example = "6a913a1ae66f803fab58605a")
            @PathVariable String id) {
        OrderResponseDTO response = orderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar pedidos com filtros", description = "Consulta a lista de pedidos aplicando filtros dinâmicos de busca (ex: partnerId, status, período).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OrderResponseDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAll(
            @ParameterObject @ModelAttribute OrderFilterRequestDTO filter) {
        List<OrderResponseDTO> response = orderService.findAllByFilter(filter);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar um pedido", description = "Altera o status de um pedido para CANCELED caso ele ainda esteja em estado passível de cancelamento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "O pedido não pode ser cancelado no seu status atual", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @Parameter(description = "ID único do pedido no MongoDB", example = "6a913a1ae66f803fab58605a")
            @PathVariable String id) {
        OrderResponseDTO response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Atualizar o status de um pedido", description = "Atualiza o status do pedido no MongoDB e publica uma notificação no tópico do Kafka.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do pedido atualizado e evento publicado no Kafka com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Status informado inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @Parameter(description = "ID único do pedido no MongoDB", example = "6a913a1ae66f803fab58605a")
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequestDTO request) {
        OrderResponseDTO response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }
}