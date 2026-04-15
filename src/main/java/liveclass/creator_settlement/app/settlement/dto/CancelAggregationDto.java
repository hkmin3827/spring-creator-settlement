package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;

public record CancelAggregationDto(BigDecimal refundAmount, long cancelCount) {}
