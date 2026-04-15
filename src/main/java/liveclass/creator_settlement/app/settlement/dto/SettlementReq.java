package liveclass.creator_settlement.app.settlement.dto;

import jakarta.validation.constraints.NotBlank;

// create or confirm
public record SettlementReq(@NotBlank String creatorId, String yearMonth) { }
