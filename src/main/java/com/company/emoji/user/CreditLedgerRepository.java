package com.company.emoji.user;

import com.company.emoji.user.entity.CreditLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditLedgerRepository extends JpaRepository<CreditLedgerEntryEntity, String> {
    List<CreditLedgerEntryEntity> findAllByUserIdOrderByCreatedAtAsc(String userId);
    List<CreditLedgerEntryEntity> findAllByGenerationTaskIdOrderByCreatedAtAsc(String generationTaskId);
    List<CreditLedgerEntryEntity> findAllByIapOrderIdOrderByCreatedAtAsc(String iapOrderId);
}
