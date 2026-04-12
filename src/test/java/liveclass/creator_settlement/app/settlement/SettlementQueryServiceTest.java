package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.SettlementRecordRes;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.SettlementRecordRepository;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementQueryServiceTest {

    @Mock SaleRecordRepository saleRecordRepository;
    @Mock CancelRecordRepository cancelRecordRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock
    SettlementRecordRepository settlementRecordRepository;
    @Mock CreatorQueryService creatorQueryService;

    @InjectMocks SettlementQueryService settlementQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settlementQueryService, "commissionRate", new BigDecimal("0.20"));
    }

    @Test
    void getMonthlySettlement_CONFIRMED_정산_SettlementRecord에서_반환() {
        Settlement settlement = Settlement.create("settle-1", "creator-1", "2025-03");
        settlement.confirm();
        SettlementRecord record = SettlementRecord.of(
                "record-1", "settle-1", "creator-1", "2025-03",
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                3L, 1L
        );

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.of(settlement));
        given(settlementRecordRepository.findBySettlementId("settle-1")).willReturn(Optional.of(record));

        SettlementRecordRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(result.creatorName()).isEqualTo("홍길동");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        verify(saleRecordRepository, never()).findByCreatorIdAndPaidAtBetween(any(), any(), any());
    }

    @Test
    void getMonthlySettlement_PAID_정산_SettlementRecord에서_반환() {
        Settlement settlement = Settlement.create("settle-1", "creator-1", "2025-03");
        settlement.confirm();
        settlement.markAsPaid();

        SettlementRecord record = SettlementRecord.of(
                "record-1", "settle-1", "creator-1", "2025-03",
                new BigDecimal("300000"), new BigDecimal("50000"), new BigDecimal("250000"),
                new BigDecimal("0.20"), new BigDecimal("50000"), new BigDecimal("200000"),
                3L, 1L
        );

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.of(settlement));
        given(settlementRecordRepository.findBySettlementId("settle-1")).willReturn(Optional.of(record));

        SettlementRecordRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.PAID);
        assertThat(result.creatorName()).isEqualTo("홍길동");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        verify(saleRecordRepository, never()).findByCreatorIdAndPaidAtBetween(any(), any(), any());
    }

    @Test
    void getMonthlySettlement_DB에_없으면_PENDING으로_계산하여_반환() {
        SaleRecord sale1 = SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("200000"), null);
        SaleRecord sale2 = SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("100000"), null);
        CancelRecord cancel = CancelRecord.of("cancel-1", "sale-1", new BigDecimal("50000"), null);

        given(creatorQueryService.getCreatorName("creator-1")).willReturn("홍길동");
        given(settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-03"))
                .willReturn(Optional.empty());
        given(saleRecordRepository.findByCreatorIdAndPaidAtBetween(eq("creator-1"), any(), any()))
                .willReturn(List.of(sale1, sale2));
        given(cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(eq("creator-1"), any(), any()))
                .willReturn(List.of(cancel));

        SettlementRecordRes result = settlementQueryService.getMonthlySettlement("creator-1", YearMonth.of(2025, 3));

        assertThat(result.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(result.creatorName()).isEqualTo("홍길동");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(result.commissionAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(result.expectedSettleAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(result.sellCount()).isEqualTo(2L);
        assertThat(result.cancelCount()).isEqualTo(1L);
    }
}
