package liveclass.creator_settlement.app.settlement.dto;

import java.time.LocalDate;

public record OperatorSettlementReq(
    LocalDate startDate,
    LocalDate endDate
) {}
