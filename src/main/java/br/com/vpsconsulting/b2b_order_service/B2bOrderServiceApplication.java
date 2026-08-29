package br.com.vpsconsulting.b2b_order_service;

import br.com.vpsconsulting.b2b_order_service.config.KafkaConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;

@SpringBootApplication
public class B2bOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(B2bOrderServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner kafkaTestRunner(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper) throws InterruptedException {
		Thread.sleep(3000);
		return args -> {
			var eventPayload = Map.of(
					"orderId", "6a913a1ae66f803fab58605a",
					"status", "APPROVED"
			);

			// Converte o objeto/mapa para JSON String antes de enviar
			String jsonMessage = objectMapper.writeValueAsString(eventPayload);

			System.out.println(">>> [KAFKA PRODUCER] Publicando mensagem no Kafka...");
			kafkaTemplate.send(KafkaConfig.ORDER_STATUS_TOPIC, "6a913a1ae66f803fab58605a", jsonMessage);
		};
	}

	@KafkaListener(topics = KafkaConfig.ORDER_STATUS_TOPIC, groupId = "b2b-order-service-group")
	public void consumeTestMessage(String message) throws InterruptedException {
		System.out.println("=================================================");
		System.out.println("<<< [KAFKA CONSUMER] Mensagem JSON recebida: " + message);
		System.out.println("=================================================");
	}

}