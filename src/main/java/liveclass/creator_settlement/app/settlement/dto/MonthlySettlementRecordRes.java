package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MonthlySettlementRecordRes(
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
    public static MonthlySettlementRecordRes from(SettlementRecord record, SettlementStatus status, String creatorName) {
        return new MonthlySettlementRecordRes(
                record.creatorId,
                creatorName,
                record.yearMonth,
                status,
                record.totalAmount,
                record.refundAmount,
                record.netAmount,
                record.commissionRate,
                record.commissionAmount.setScale(0, RoundingMode.DOWN),
                record.settleAmount.setScale(0, RoundingMode.UP),
                record.sellCount,
                record.cancelCount
        );
    }
}
