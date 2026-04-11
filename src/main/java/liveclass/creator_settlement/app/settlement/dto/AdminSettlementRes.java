package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.vo.Money;

import java.math.BigDecimal;
import java.util.List;

public record AdminSettlementRes(
    List<CreatorSettlementEntry> entries,
    BigDecimal totalSettlementAmount
) {
    public static AdminSettlementRes from(List<CreatorSettlementEntry> entries, Money totalSettlementAmount) {
        BigDecimal stripped = totalSettlementAmount.amount().stripTrailingZeros();
        BigDecimal total = stripped.scale() < 0 ? stripped.setScale(0) : stripped;
        return new AdminSettlementRes(entries, total);
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
                Money commissionAmount, Money expectedSettleAmount,
                long sellCount, long cancelCount) {
            return new CreatorSettlementEntry(
                    creatorId, creatorName,
                    strip(totalAmount.amount()),
                    strip(refundAmount.amount()),
                    strip(netAmount.amount()),
                    strip(commissionAmount.amount()),
                    strip(expectedSettleAmount.amount()),
                    sellCount, cancelCount
            );
        }

        private static BigDecimal strip(BigDecimal value) {
            BigDecimal stripped = value.stripTrailingZeros();
            return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
        }
    }
}
