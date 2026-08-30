package br.com.vpsconsulting.b2b_order_service.repository;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends MongoRepository<Partner, String>, PartnerRepositoryCustom {

    boolean existsByCnpj(String cnpj);

}