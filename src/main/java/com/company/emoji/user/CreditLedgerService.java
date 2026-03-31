package com.company.emoji.user;

import com.company.emoji.user.domain.CreditLedgerEntryType;
import com.company.emoji.user.entity.CreditLedgerEntryEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreditLedgerService {
    private final CreditLedgerRepository creditLedgerRepository;

    public CreditLedgerService(CreditLedgerRepository creditLedgerRepository) {
        this.creditLedgerRepository = creditLedgerRepository;
    }

    @Transactional
    public void record(
            String userId,
            CreditLedgerEntryType entryType,
            int availableDelta,
            int frozenDelta,
            int balanceAfterAvailable,
            int balanceAfterFrozen,
            String generationTaskId,
            String iapOrderId,
            String description
    ) {
        CreditLedgerEntryEntity entry = new CreditLedgerEntryEntity();
        entry.setId("ledger_" + UUID.randomUUID().toString().replace("-", ""));
        entry.setUserId(userId);
        entry.setEntryType(entryType.name());
        entry.setAvailableDelta(availableDelta);
        entry.setFrozenDelta(frozenDelta);
        entry.setBalanceAfterAvailable(balanceAfterAvailable);
        entry.setBalanceAfterFrozen(balanceAfterFrozen);
        entry.setGenerationTaskId(generationTaskId);
        entry.setIapOrderId(iapOrderId);
        entry.setDescription(description);
        entry.setCreatedAt(Instant.now());
        creditLedgerRepository.save(entry);
    }
}
