package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;

public record SettlementCalculation(
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        BigDecimal commissionRate,
        BigDecimal strippedCommissionAmount,
        BigDecimal expectedSettleAmount,
        long sellCount,
        long cancelCount
) {}