package br.com.vpsconsulting.b2b_order_service.repository;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByPartnerId(String partnerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCreatedAtBetween(Instant startDate, Instant endDate);

    List<Order> findByStatusAndCreatedAtBetween(OrderStatus status, Instant startDate, Instant endDate);

    // TODO: add dynamic query with filters
}