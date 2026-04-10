package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;

public record SettlementRes(
    String creatorId,
    String yearMonth,
    SettlementStatus status,
    BigDecimal totalAmount,
    BigDecimal refundAmount,
    BigDecimal netAmount, // 순 판매 금액
    BigDecimal commissionRate,  // 수수료
    BigDecimal commissionAmount,
    BigDecimal settlementAmount,
    long sellCount,
    long cancelCount
) {
    public static SettlementRes from(Settlement settlement) {
        return new SettlementRes(
                settlement.creatorId,
                settlement.yearMonth,
                settlement.status,
                settlement.amount,
                settlement.refundAmount,
                settlement.netAmount,
                settlement.commissionRate,
                settlement.commissionAmount,
                settlement.settlementAmount,
                settlement.sellCount,
                settlement.cancelCount
        );
    }
}
