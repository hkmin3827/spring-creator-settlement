package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MonthlySettlementRes(
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
    public static MonthlySettlementRes from(Settlement sm, String creatorName) {
        return new MonthlySettlementRes(
                sm.creatorId,
                creatorName,
                sm.yearMonth,
                sm.status,
                sm.totalAmount,
                sm.refundAmount,
                sm.netAmount,
                sm.commissionRate,
                sm.commissionAmount.setScale(0, RoundingMode.DOWN),
                sm.settleAmount.setScale(0, RoundingMode.UP),
                sm.sellCount,
                sm.cancelCount
        );
    }
}
