package br.com.vpsconsulting.b2b_order_service.repository;

import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class PartnerRepositoryCustomImpl implements PartnerRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public long deductCreditLimit(String partnerId, BigDecimal amount) {
        Query query = new Query(
                Criteria.where("_id").is(partnerId)
                        .and("availableLimit").gte(amount)
        );

        Update update = new Update().inc("availableLimit", amount.negate());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Partner.class);
        return result.getModifiedCount();
    }

    @Override
    public long refundCreditLimit(String partnerId, BigDecimal amount) {
        Query query = new Query(Criteria.where("_id").is(partnerId));
        Update update = new Update().inc("availableLimit", amount);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Partner.class);
        return result.getModifiedCount();
    }
}