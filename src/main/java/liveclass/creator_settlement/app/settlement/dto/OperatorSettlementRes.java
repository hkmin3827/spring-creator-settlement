package liveclass.creator_settlement.app.settlement.dto;

import liveclass.creator_settlement.domain.vo.Money;
import liveclass.creator_settlement.global.page.PageResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public record OperatorSettlementRes(
    PageResponse<CreatorSettlementEntry> entries,
    BigDecimal totalSettlementAmount
) {
    public static OperatorSettlementRes from(Page<CreatorSettlementEntry> entries, BigDecimal totalSettlementAmount) {
        return new OperatorSettlementRes(PageResponse.from(entries), totalSettlementAmount);
    }

    public record CreatorSettlementEntry(
        String creatorId,
        String creatorName,
        BigDecimal settleAmount
    ) {
        public static CreatorSettlementEntry of(
                String creatorId, String creatorName, Money settleAmount) {
            return new CreatorSettlementEntry(
                    creatorId, creatorName, settleAmount.amount()
            );
        }

    }
}
