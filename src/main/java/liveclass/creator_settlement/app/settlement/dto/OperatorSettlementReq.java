package liveclass.creator_settlement.app.settlement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OperatorSettlementReq(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
