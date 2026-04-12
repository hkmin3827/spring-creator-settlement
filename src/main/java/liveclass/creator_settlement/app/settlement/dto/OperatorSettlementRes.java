package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.vo.Money;

import java.math.BigDecimal;
import java.util.List;

public record OperatorSettlementRes(
    List<CreatorSettlementEntry> entries,
    BigDecimal totalSettlementAmount
) {
    public static OperatorSettlementRes from(List<CreatorSettlementEntry> entries, BigDecimal totalSettlementAmount) {
        return new OperatorSettlementRes(entries, totalSettlementAmount);
    }

    public record CreatorSettlementEntry(
        String creatorId,
        String creatorName,
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        BigDecimal commissionAmount,
        BigDecimal expectedSettleAmount,
        long sellCount,
        long cancelCount
    ) {
        public static CreatorSettlementEntry of(
                String creatorId, String creatorName,
                Money totalAmount, Money refundAmount, Money netAmount,
                Money finalCommissionAmount, Money expectedSettleAmount,
                long sellCount, long cancelCount) {
            return new CreatorSettlementEntry(
                    creatorId, creatorName,
                    totalAmount.amount(),
                    refundAmount.amount(),
                    netAmount.amount(),
                    finalCommissionAmount.amount(),
                    expectedSettleAmount.amount(),
                    sellCount, cancelCount
            );
        }

    }
}
