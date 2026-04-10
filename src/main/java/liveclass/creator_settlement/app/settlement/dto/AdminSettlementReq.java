package liveclass.creator_settlement.app.settlement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminSettlementReq(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
