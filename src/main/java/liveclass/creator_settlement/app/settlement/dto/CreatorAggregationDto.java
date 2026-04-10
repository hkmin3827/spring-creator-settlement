package liveclass.creator_settlement.app.settlement.dto;

import java.math.BigDecimal;

public record CreatorAggregationDto(String creatorId, BigDecimal totalAmount, Long count) {}
