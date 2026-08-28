package br.com.vpsconsulting.b2b_order_service.repository;

import java.math.BigDecimal;

public interface PartnerRepositoryCustom {
    long deductCreditLimit(String partnerId, BigDecimal amount);
    long refundCreditLimit(String partnerId, BigDecimal amount);
}