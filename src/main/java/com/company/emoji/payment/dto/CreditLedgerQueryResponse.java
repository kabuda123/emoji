package com.company.emoji.payment.dto;

import java.util.List;

public record CreditLedgerQueryResponse(
        List<CreditLedgerEntryResponse> entries,
        int total
) {
}
