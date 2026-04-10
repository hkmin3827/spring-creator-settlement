package liveclass.creator_settlement.app.saleRecord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleRecordCreateReq(
    @NotBlank String courseId,
    @NotBlank String studentId,
    @NotNull @Positive BigDecimal amount,
    @NotNull LocalDateTime paidAt
) {}
