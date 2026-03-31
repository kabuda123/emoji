package com.company.emoji.payment;

import com.company.emoji.payment.entity.IapOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IapOrderRepository extends JpaRepository<IapOrderEntity, String> {
    Optional<IapOrderEntity> findByTransactionId(String transactionId);
}
