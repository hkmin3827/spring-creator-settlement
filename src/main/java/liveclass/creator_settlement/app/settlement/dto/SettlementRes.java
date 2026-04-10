package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;

public record SettlementRes(
    BigDecimal totalAmount,
    BigDecimal refundAmount,
    BigDecimal netAmount, // 순 판매 금액
    Double commission  // 수수료
) {}
