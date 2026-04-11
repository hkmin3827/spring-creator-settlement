package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.SettlementLog;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;

public record SettlementLogRes(
    String creatorId,
    String creatorName,
    String yearMonth,
    SettlementStatus status,
    BigDecimal totalAmount,
    BigDecimal refundAmount,
    BigDecimal netAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    BigDecimal expectedSettleAmount,
    long sellCount,
    long cancelCount
) {
    public static SettlementLogRes from(SettlementLog log, SettlementStatus status, String creatorName) {
        return new SettlementLogRes(
                log.creatorId,
                creatorName,
                log.yearMonth,
                status,
                strip(log.totalAmount),
                strip(log.refundAmount),
                strip(log.netAmount),
                log.commissionRate,
                strip(log.commissionAmount),
                strip(log.expectedSettleAmount),
                log.sellCount,
                log.cancelCount
        );
    }

    private static java.math.BigDecimal strip(java.math.BigDecimal value) {
        java.math.BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
