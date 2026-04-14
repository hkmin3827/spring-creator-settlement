package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.MonthlySettlementRes;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementQueryServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock CreatorQueryService creatorQueryService;
    @Mock SaleRecordRepository saleRecordRepository;
    @Mock CancelRecordRepository cancelRecordRepository;
    @Mock IdGenerator idGenerator;

    SettlementService settlementService;

    @InjectMocks SettlementQueryService settlementQueryService;

    @BeforeEach
    void setUp() {
        settlementService = spy(new SettlementService(saleRecordRepository, cancelRecordRepository, idGenerator, settlementRepository));
        ReflectionTestUtils.setField(settlementService, "commissionRate", new BigDecimal("0.20"));
        ReflectionTestUtils.setField(settlementQueryService, "settlementService", settlementService);
    }

    @Test
    void getMonthlySettlement_현재월_실시간_계산_반환() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime paidAt = currentMonth.atDay(5).atStartOfDay();

        given(saleRecordRepository.findByCreatorIdAndPaidAtBetween(eq("creator-1"), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("200000"), paidAt),
                        SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("100000"), paidAt)
                )));
        given(cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(eq("creator-1"), any(), any()))
                .willReturn(List.of(
                        CancelRecord.of("cancel-1", "sale-2", paidAt, new BigDecimal("50000"), paidAt.plusDays(2))
                ));
        given(creatorQueryService.getCreatorName("creator-1")).willReturn("김강사");

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", currentMonth);

        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorName()).isEqualTo("김강사");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.sellCount()).isEqualTo(2L);
        assertThat(result.cancelCount()).isEqualTo(1L);
        verify(settlementRepository, never()).findByCreatorIdAndYearMonth(any(), any());
    }

    @Test
    void getMonthlySettlement_CONFIRMED_정산_Settlement에서_반환() {
        Settlement settlement = Settlement.create(
                "settle-1", "creator-1", "2025-03",
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                3L, 1L
        );
        settlement.confirm();

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("김강사");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.of(settlement));

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(result.creatorName()).isEqualTo("김강사");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        verify(settlementService, never()).calculate(any(), any());
    }

    @Test
    void getMonthlySettlement_DB에_없으면_PENDING으로_계산하여_반환() {
        LocalDateTime paidAt = LocalDateTime.of(2025, 3, 10, 10, 0);

        given(saleRecordRepository.findByCreatorIdAndPaidAtBetween(eq("creator-1"), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("200000"), paidAt),
                        SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("100000"), paidAt)
                )));
        given(cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(eq("creator-1"), any(), any()))
                .willReturn(List.of(
                        CancelRecord.of("cancel-1", "sale-2", paidAt, new BigDecimal("50000"), paidAt.plusDays(2))
                ));
        given(creatorQueryService.getCreatorName("creator-1")).willReturn("김강사");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.empty());

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorName()).isEqualTo("김강사");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.sellCount()).isEqualTo(2L);
        assertThat(result.cancelCount()).isEqualTo(1L);
    }

    @Test
    void getMonthlySettlement_미래월_입력_실패() {
        assertThatThrownBy(() -> settlementQueryService.getMonthlySettlement("creator-1", YearMonth.now().plusMonths(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.YEAR_MONTH_BAD_REQUEST);
    }
}
