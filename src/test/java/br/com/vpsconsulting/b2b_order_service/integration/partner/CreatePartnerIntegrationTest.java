package br.com.vpsconsulting.b2b_order_service.integration.partner;

import br.com.vpsconsulting.b2b_order_service.integration.AbstractIntegrationTest;
import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.repository.PartnerRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class CreatePartnerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @AfterEach
    void tearDown() {
        // Limpa o banco entre execucoes para manter os testes isolados
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar um novo parceiro com sucesso")
    void shouldCreatePartnerSuccessfully() {
        PartnerRequestDTO request = new PartnerRequestDTO();
        request.setName("Empresa Parceira LTDA");
        request.setCnpj("1125544550001");
        request.setCreditLimit(new BigDecimal("200000"));

        // Cadastra um parceiro
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/partners")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("cnpj", equalTo("1125544550001"))
                .body("name", equalTo("Empresa Parceira LTDA"));

        // Garante que o parceiro esta presente no BD
        boolean existsInDb = partnerRepository.existsByCnpj("1125544550001");
        assertThat(existsInDb).isTrue();
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict ao tentar cadastrar um CNPJ já existente")
    void shouldReturn409WhenCnpjAlreadyExists() {
        PartnerRequestDTO request = new PartnerRequestDTO();
        request.setName("Empresa Parceira LTDA");
        request.setCnpj("1125544550001");
        request.setCreditLimit(new BigDecimal("200000"));

        // Cadastra parceiro
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/partners")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        // Tenta cadastrar um segundo parceiro com o mesmo CNPJ
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/partners")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("message", equalTo("Já existe um parceiro cadastrado com o CNPJ: 1125544550001"));
    }
}