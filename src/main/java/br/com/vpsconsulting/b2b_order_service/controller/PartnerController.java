package br.com.vpsconsulting.b2b_order_service.controller;

import br.com.vpsconsulting.b2b_order_service.dto.request.PartnerRequestDTO;
import br.com.vpsconsulting.b2b_order_service.dto.response.PartnerResponseDTO;
import br.com.vpsconsulting.b2b_order_service.service.PartnerService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partners", description = "Endpoints para cadastro e consulta de parceiros comerciais")
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "Cadastrar um novo parceiro", description = "Cria um novo parceiro comercial B2B no MongoDB com limite de crédito.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Parceiro cadastrado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou CNPJ já existente", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PartnerResponseDTO> createPartner(@Valid @RequestBody PartnerRequestDTO request) {
        PartnerResponseDTO response = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Buscar parceiro por ID", description = "Retorna os detalhes de um parceiro cadastrado no MongoDB pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parceiro encontrado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Parceiro não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponseDTO> findById(
            @Parameter(description = "ID do parceiro no MongoDB", example = "66d1f8b2e4b0123456789abc")
            @PathVariable String id) {
        PartnerResponseDTO response = partnerService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todos os parceiros", description = "Retorna a lista completa de parceiros comerciais cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PartnerResponseDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<PartnerResponseDTO>> findAll() {
        List<PartnerResponseDTO> response = partnerService.findAll();
        return ResponseEntity.ok(response);
    }
}