package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.MonthlySettlementRes;
import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementQueryServiceTest {

    @Mock SettlementRepository settlementRepository;
    @Mock CreatorQueryService creatorQueryService;
    @Mock SettlementService settlementService;
    @InjectMocks SettlementQueryService settlementQueryService;

    @Test
    void getMonthlySettlement_현재월_실시간_계산_반환() {
        YearMonth currentMonth = YearMonth.now();
        SettlementCalculation calc = new SettlementCalculation(
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                2L, 1L
        );

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementService.calculate("creator-1", currentMonth)).willReturn(calc);

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", currentMonth);

        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorName()).isEqualTo("홍길동");
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

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.of(settlement));

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(result.creatorName()).isEqualTo("홍길동");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        verify(settlementService, never()).calculate(any(), any());
    }

    @Test
    void getMonthlySettlement_PAID_정산_Settlement에서_반환() {
        Settlement settlement = Settlement.create(
                "settle-1", "creator-1", "2025-03",
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                3L, 1L
        );
        settlement.confirm();
        settlement.markAsPaid();

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.of(settlement));

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.PAID);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        verify(settlementService, never()).calculate(any(), any());
    }

    @Test
    void getMonthlySettlement_DB에_없으면_PENDING으로_계산하여_반환() {
        SettlementCalculation calc = new SettlementCalculation(
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                2L, 1L
        );

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.empty());
        given(settlementService.calculate("creator-1", YearMonth.of(2025, 3))).willReturn(calc);

        MonthlySettlementRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorName()).isEqualTo("홍길동");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.sellCount()).isEqualTo(2L);
        assertThat(result.cancelCount()).isEqualTo(1L);
    }
}
