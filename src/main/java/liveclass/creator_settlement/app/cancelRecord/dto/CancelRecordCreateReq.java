package liveclass.creator_settlement.app.cancelRecord.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancelRecordCreateReq(
    @NotBlank String saleRecordId,
    @NotNull @Positive @Digits(integer = 6, fraction = 2) BigDecimal refundAmount,
    @NotNull LocalDateTime cancelledAt
) {}
