package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.config.KafkaConfig;
import br.com.vpsconsulting.b2b_order_service.dto.event.OrderStatusUpdatedEvent;
import br.com.vpsconsulting.b2b_order_service.exception.EventSerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            log.info("Publicando evento OrderStatusUpdatedEvent no Kafka para o pedido ID {}: {}", event.getOrderId(), jsonPayload);

            kafkaTemplate.send(KafkaConfig.ORDER_STATUS_TOPIC, event.getOrderId(), jsonPayload)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Evento enviado com SUCESSO para a partição [{}] com offset [{}]",
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("FALHA ao enviar evento para o Kafka no pedido ID {}: {}", event.getOrderId(), ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento de atualização de status do pedido ID {}", event.getOrderId(), e);
            throw new EventSerializationException("Erro ao serializar evento do Kafka", e);
        }
    }
}