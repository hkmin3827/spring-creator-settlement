package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

public record OperatorSettlementRes(
    List<CreatorSettlementEntry> entries,
    BigDecimal totalSettlementAmount
) {
    public record CreatorSettlementEntry(
        String creatorId,
        String creatorName,
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        BigDecimal commissionAmount,
        BigDecimal settlementAmount,
        long sellCount,
        long cancelCount
    ) {}
}
