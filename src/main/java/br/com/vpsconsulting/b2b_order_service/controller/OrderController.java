package br.com.vpsconsulting.b2b_order_service.controller;

import br.com.vpsconsulting.b2b_order_service.dto.request.OrderFilterRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.UpdateOrderStatusRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.OrderResponseDTO;
import br.com.vpsconsulting.b2b_order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO request) {
        OrderResponseDTO createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> findById(@PathVariable String id) {
        OrderResponseDTO response = orderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAll(@ModelAttribute OrderFilterRequestDTO filter) {
        List<OrderResponseDTO> response = orderService.findAllByFilter(filter);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable String id) {
        OrderResponseDTO response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequestDTO request) {
        OrderResponseDTO response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }
}