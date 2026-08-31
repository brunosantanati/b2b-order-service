package br.com.vpsconsulting.b2b_order_service.integration.order;

import br.com.vpsconsulting.b2b_order_service.config.KafkaConfig;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderItemRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.OrderRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.integration.AbstractIntegrationTest;
import br.com.vpsconsulting.b2b_order_service.repository.OrderRepository;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.ConsumerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class CreateOrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar uma novo pedido com sucesso e publicar evento no Kafka")
    void shouldCreateOrderSuccessfully() {
        // 1. Pré-requisito: Cria um parceiro no banco para ter um ID válido
        var partnerRequest = new PartnerRequestDTO();
        partnerRequest.setCnpj("00000000000191");
        partnerRequest.setName("Empresa Compradora LTDA");
        partnerRequest.setCreditLimit(new BigDecimal("200000.00"));

        String partnerId = given()
                .contentType(ContentType.JSON)
                .body(partnerRequest)
                .post("/partners")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");

        // 2. Prepara a requisição do novo pedido
        var orderItem = new OrderItemRequestDTO();
        orderItem.setProductId("PROD-12345");
        orderItem.setQuantity(10);
        orderItem.setUnitPrice(new BigDecimal("150.00"));

        var orderRequest = new OrderRequestDTO();
        orderRequest.setPartnerId(partnerId);
        orderRequest.setItems(List.of(orderItem));

        // 3. Executa a criação do pedido
        String orderId = given()
                .contentType(ContentType.JSON)
                .body(orderRequest)
                .when()
                .post("/orders")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("partnerId", equalTo(partnerId))
                .body("totalAmount", equalTo(1500.0F)) // 10 * 150.00
                .body("status", equalTo("PENDING"))
                .extract()
                .path("id");

        // 4. Verifica se o pedido existe no BD
        var savedOrder = orderRepository.findById(orderId);
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getPartnerId()).isEqualTo(partnerId);
        assertThat(savedOrder.get().getTotalAmount()).isEqualByComparingTo("1500.00");

        // 5. Verifica se ocorreu a publicação da mensagem no Kafka
        try (Consumer<String, String> consumer = createKafkaConsumer(KafkaConfig.ORDER_STATUS_TOPIC)) {
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                assertThat(records.isEmpty()).isFalse();

                ConsumerRecord<String, String> record = records.iterator().next();
                assertThat(record.value()).contains(orderId);
                assertThat(record.value()).contains(partnerId);
            });
        }
    }

    private Consumer<String, String> createKafkaConsumer(String topic) {
        Consumer<String, String> consumer = consumerFactory.createConsumer(
                "test-group-" + UUID.randomUUID(),
                "clientId-test"
        );
        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }
}
