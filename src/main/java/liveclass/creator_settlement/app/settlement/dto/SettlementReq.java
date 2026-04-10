package liveclass.creator_settlement.app.settlement.dto;

import java.time.YearMonth;

public record SettlementReq(
    String creatorId,
    YearMonth targetMonth   // 2025-03
) { }
