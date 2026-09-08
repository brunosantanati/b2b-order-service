package br.com.vpsconsulting.b2b_order_service.service;

import br.com.vpsconsulting.b2b_order_service.config.KafkaConfig;
import br.com.vpsconsulting.b2b_order_service.dto.event.OrderStatusUpdatedEvent;
import br.com.vpsconsulting.b2b_order_service.exception.SendEventException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

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

            var result = kafkaTemplate.send(KafkaConfig.ORDER_STATUS_TOPIC, event.getOrderId(), jsonPayload).get();

            log.info("Evento enviado com SUCESSO para a partição [{}] com offset [{}]",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar evento de atualização de status do pedido ID {}", event.getOrderId(), e);
            throw new SendEventException("Erro ao serializar evento do Kafka", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrompida ao enviar evento para o Kafka no pedido ID {}", event.getOrderId(), e);
            throw new SendEventException("Falha de envio ao Kafka (Thread Interrupted)", e);
        } catch (ExecutionException e) {
            log.error("FALHA ao enviar evento para o Kafka no pedido ID {}: {}", event.getOrderId(), e.getCause().getMessage());
            throw new SendEventException("Falha ao notificar mensageria Kafka. Operação abortada.", e.getCause());
        }
    }
}