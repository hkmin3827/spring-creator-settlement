package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;

public record SettlementRes(
    String creatorId,
    String creatorName,
    String yearMonth,
    SettlementStatus status,
    BigDecimal totalAmount,
    BigDecimal refundAmount,
    BigDecimal netAmount, // 순 판매 금액
    BigDecimal commissionRate,  // 수수료
    BigDecimal commissionAmount,
    BigDecimal expectedSettleAmount,
    long sellCount,
    long cancelCount
) {
    public static SettlementRes from(Settlement settlement, String creatorName) {
        return new SettlementRes(
                settlement.creatorId,
                creatorName,
                settlement.yearMonth,
                settlement.status,
                settlement.totalAmount,
                settlement.refundAmount,
                settlement.netAmount,
                settlement.commissionRate,
                settlement.commissionAmount,
                settlement.expectedSettleAmount,
                settlement.sellCount,
                settlement.cancelCount
        );
    }
}
