package br.com.vpsconsulting.b2b_order_service.repository;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PartnerRepository extends MongoRepository<Partner, String> {

    Optional<Partner> findByCnpj(String cnpj);

    @Query("{ '_id': ?0, 'availableLimit': { $gte: ?1 } }")
    @Update("{ '$inc': { 'availableLimit': -?1 } }")
    long deductCreditLimit(String partnerId, BigDecimal amount);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'availableLimit': ?1 } }")
    long refundCreditLimit(String partnerId, BigDecimal amount);
}