package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.CancelAggregationDto;
import liveclass.creator_settlement.app.settlement.dto.SaleAggregationDto;
import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
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
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock SaleRecordRepository saleRecordRepository;
    @Mock CancelRecordRepository cancelRecordRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock IdGenerator idGenerator;
    @InjectMocks SettlementService settlementService;

    private static final YearMonth LAST_MONTH = YearMonth.now().minusMonths(1);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settlementService, "commissionRate", new BigDecimal("0.20"));
    }

    private Settlement makeSettlement(String id, String creatorId) {
        return Settlement.create(
                id, creatorId, "2025-03",
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("300000"),
                new BigDecimal("0.20"), new BigDecimal("60000"), new BigDecimal("240000"),
                3L, 0L
        );
    }

    @Test
    void createPending_정상_생성() {
        given(settlementRepository.existsByCreatorIdAndYearMonth("creator-1", LAST_MONTH.toString())).willReturn(false);
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(new BigDecimal("300000"), 2L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(null, 0L));
        given(idGenerator.generateSettlementId()).willReturn("settlement-10");
        given(settlementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = settlementService.createPending("creator-1", LAST_MONTH);

        assertThat(result).isEqualTo("settlement-10");
        verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    void createPending_현재월_요청이면_예외() {
        assertThatThrownBy(() -> settlementService.createPending("creator-1", YearMonth.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SETTLEMENT_CREATE_OR_CONFIRMED_YEAR_MONTH);
    }

    @Test
    void createPending_미래월_요청이면_예외() {
        assertThatThrownBy(() -> settlementService.createPending("creator-1", YearMonth.now().plusMonths(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SETTLEMENT_CREATE_OR_CONFIRMED_YEAR_MONTH);
    }

    @Test
    void createPending_이미_존재하는_정산이면_예외() {
        given(settlementRepository.existsByCreatorIdAndYearMonth("creator-1", LAST_MONTH.toString())).willReturn(true);

        assertThatThrownBy(() -> settlementService.createPending("creator-1", LAST_MONTH))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
    }

    @Test
    void confirmPending_PENDING_정산_CONFIRMED_으로_변경() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03")).willReturn(Optional.of(settlement));

        settlementService.confirmPending("creator-1", YearMonth.parse("2025-03"));

        assertThat(settlement.status).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(settlement.confirmedAt).isNotNull();
    }

    @Test
    void confirmPending_정산을_찾을_수_없으면_예외() {
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03")).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.confirmPending("creator-1", YearMonth.parse("2025-03")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    void confirmPending_이미_CONFIRMED_정산이면_예외() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        settlement.confirm();
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03")).willReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementService.confirmPending("creator-1", YearMonth.parse("2025-03")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CONFIRMED_SETTLEMENT);
    }

    @Test
    void confirmPending_PAID_정산을_confirm_하면_예외() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        settlement.confirm();
        settlement.markAsPaid();
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03")).willReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementService.confirmPending("creator-1", YearMonth.parse("2025-03")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CONFIRMED_SETTLEMENT);
    }

    @Test
    void markAsPaid_CONFIRMED_정산_PAID_로_변경() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        settlement.confirm();
        given(settlementRepository.findById("settle-1")).willReturn(Optional.of(settlement));

        settlementService.markAsPaid("settle-1");

        assertThat(settlement.status).isEqualTo(SettlementStatus.PAID);
        assertThat(settlement.paidAt).isNotNull();
    }

    @Test
    void markAsPaid_정산을_찾을_수_없으면_예외() {
        given(settlementRepository.findById("settle-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.markAsPaid("settle-999"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    void markAsPaid_이미_PAID_정산이면_예외() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        settlement.confirm();
        settlement.markAsPaid();
        given(settlementRepository.findById("settle-1")).willReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementService.markAsPaid("settle-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_PAID_SETTLEMENT);
    }

    @Test
    void markAsPaid_PENDING_정산_결제_상태_변경_예외() {
        Settlement settlement = makeSettlement("settle-1", "creator-1");
        given(settlementRepository.findById("settle-1")).willReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementService.markAsPaid("settle-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SETTLEMENT_STATUS);
    }

    @Test
    void bulkAsPaidMonthly_정상_일괄_처리() {
        YearMonth yearMonth = YearMonth.of(2025, 3);
        given(settlementRepository.bulkUpdateStatus("2025-03", SettlementStatus.PAID, SettlementStatus.CONFIRMED, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
                .willReturn(5);

        int result = settlementService.bulkAsPaidMonthly(yearMonth);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void bulkAsPaidMonthly_CONFIRMED_정산이_없으면_예외() {
        YearMonth yearMonth = YearMonth.of(2025, 3);
        given(settlementRepository.bulkUpdateStatus("2025-03", SettlementStatus.PAID, SettlementStatus.CONFIRMED, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
                .willReturn(0);

        assertThatThrownBy(() -> settlementService.bulkAsPaidMonthly(yearMonth))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NO_CONFIRMED_SETTLEMENTS);
    }

    @Test
    void bulkAsPaidMonthly_쿼리_타임아웃_발생시_예외() {
        given(settlementRepository.bulkUpdateStatus(any(), any(), any(), any()))
                .willThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> settlementService.bulkAsPaidMonthly(YearMonth.of(2025, 3)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QUERY_TIMEOUT);
    }

    @Test
    void calculate_판매내역만_있으면_정상_계산() {
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(new BigDecimal("300000"), 2L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(null, 0L));

        SettlementCalculation calc = settlementService.calculate("creator-1", YearMonth.of(2025, 3));

        assertThat(calc.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(calc.refundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.netAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(calc.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("240000"));
        assertThat(calc.sellCount()).isEqualTo(2L);
        assertThat(calc.cancelCount()).isEqualTo(0L);
    }

    @Test
    void calculate_판매와_취소_모두_있으면_환불_차감_후_계산() {
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(new BigDecimal("300000"), 2L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(new BigDecimal("100000"), 1L));

        SettlementCalculation calc = settlementService.calculate("creator-1", YearMonth.of(2025, 3));

        assertThat(calc.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(calc.refundAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(calc.netAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(calc.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("160000"));
        assertThat(calc.sellCount()).isEqualTo(2L);
        assertThat(calc.cancelCount()).isEqualTo(1L);
    }

    @Test
    void calculate_판매내역이_없으면_모두_0으로_반환() {
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(null, 0L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(null, 0L));

        SettlementCalculation calc = settlementService.calculate("creator-1", YearMonth.of(2025, 3));

        assertThat(calc.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.expectedSettleAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.sellCount()).isEqualTo(0L);
        assertThat(calc.cancelCount()).isEqualTo(0L);
    }

    @Test
    void calculate_수수료는_소수점_버림으로_계산() {
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(new BigDecimal("150001"), 1L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(null, 0L));

        SettlementCalculation calc = settlementService.calculate("creator-1", YearMonth.of(2025, 3));

        assertThat(calc.strippedCommissionAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(calc.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("120001"));
    }

    @Test
    void calculate_취소내역의_paidAt이_해당월_범위_밖이면_환불_제외() {
        given(saleRecordRepository.aggregateSalesForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new SaleAggregationDto(new BigDecimal("200000"), 1L));
        given(cancelRecordRepository.aggregateCancelsForSettlement(eq("creator-1"), any(), any()))
                .willReturn(new CancelAggregationDto(null, 1L));

        SettlementCalculation calc = settlementService.calculate("creator-1", YearMonth.of(2025, 3));

        assertThat(calc.refundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calc.netAmount()).isEqualByComparingTo(new BigDecimal("200000"));
    }
}
