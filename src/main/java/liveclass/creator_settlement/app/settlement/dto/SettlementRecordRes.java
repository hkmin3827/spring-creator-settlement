package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;

public record SettlementRecordRes(
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
    public static SettlementRecordRes from(SettlementRecord record, SettlementStatus status, String creatorName) {
        return new SettlementRecordRes(
                record.creatorId,
                creatorName,
                record.yearMonth,
                status,
                strip(record.totalAmount),
                strip(record.refundAmount),
                strip(record.netAmount),
                record.commissionRate,
                strip(record.commissionAmount),
                strip(record.expectedSettleAmount),
                record.sellCount,
                record.cancelCount
        );
    }

    private static java.math.BigDecimal strip(java.math.BigDecimal value) {
        java.math.BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
