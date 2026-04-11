package liveclass.creator_settlement.app.settlement.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.YearMonth;

// create or confirm
public record SettlementReq(@NotBlank String creatorId, YearMonth yearMonth) { }
