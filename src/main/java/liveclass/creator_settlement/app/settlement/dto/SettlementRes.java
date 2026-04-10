package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.settlement.SettlementLog;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;

import java.math.BigDecimal;

public record SettlementRes(
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
    public static SettlementRes from(SettlementLog log, SettlementStatus status, String creatorName) {
        return new SettlementRes(
                log.creatorId,
                creatorName,
                log.yearMonth,
                status,
                log.totalAmount,
                log.refundAmount,
                log.netAmount,
                log.commissionRate,
                log.commissionAmount,
                log.expectedSettleAmount,
                log.sellCount,
                log.cancelCount
        );
    }
}
