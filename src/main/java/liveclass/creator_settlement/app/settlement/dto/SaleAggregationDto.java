package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;

public record SaleAggregationDto(BigDecimal totalAmount, long sellCount) {}
