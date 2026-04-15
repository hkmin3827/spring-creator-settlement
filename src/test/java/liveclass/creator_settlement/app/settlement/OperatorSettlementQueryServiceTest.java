package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.OperatorSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OperatorSettlementQueryServiceTest {
    @Mock
    SaleRecordRepository saleRecordRepository;
    @Mock
    CancelRecordRepository cancelRecordRepository;
    @Mock
    CreatorQueryService creatorQueryService;
    @InjectMocks
    OperatorSettlementQueryService operatorSettlementQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operatorSettlementQueryService, "commissionRate", new BigDecimal("0.20"));
    }

    @Test
    void getOperatorAggregate_판매_취소_집계_및_수수료_계산() {
        given(saleRecordRepository.aggregateSalesByCreatorInRange(any(), any())).willReturn(List.of(
                new CreatorAggregationDto("creator-1", new BigDecimal("500000")),
                new CreatorAggregationDto("creator-2", new BigDecimal("200000"))
        ));
        given(cancelRecordRepository.aggregateCancelsByCreatorInRange(any(), any())).willReturn(List.of(
                new CreatorAggregationDto("creator-1", new BigDecimal("100000"))
        ));
        given(creatorQueryService.getAllCreatorNames())
                .willReturn(Map.of("creator-1", "최강사", "creator-2", "서강사"));

        OperatorSettlementRes result = operatorSettlementQueryService.getOperatorAggregate(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), PageRequest.of(0, Integer.MAX_VALUE)
        );

        assertThat(result.entries().content()).hasSize(2);

        OperatorSettlementRes.CreatorSettlementEntry creator1 = result.entries().content().stream()
                .filter(e -> "creator-1".equals(e.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator1.creatorName()).isEqualTo("최강사");
        assertThat(creator1.settleAmount()).isEqualByComparingTo(new BigDecimal("320000.00"));

        OperatorSettlementRes.CreatorSettlementEntry creator2 = result.entries().content().stream()
                .filter(e -> "creator-2".equals(e.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator2.settleAmount()).isEqualByComparingTo(new BigDecimal("160000.00"));

        assertThat(result.totalSettlementAmount()).isEqualByComparingTo(new BigDecimal("480000.00"));
    }

    @Test
    void getOperatorAggregate_판매내역_없는_크리에이터도_0으로_응답_반환() {
        given(saleRecordRepository.aggregateSalesByCreatorInRange(any(), any())).willReturn(List.of(
                new CreatorAggregationDto("creator-1", new BigDecimal("300000"))
        ));
        given(cancelRecordRepository.aggregateCancelsByCreatorInRange(any(), any())).willReturn(List.<CreatorAggregationDto>of());
        given(creatorQueryService.getAllCreatorNames())
                .willReturn(Map.of("creator-1", "최강사", "creator-2", "서강사"));

        OperatorSettlementRes result = operatorSettlementQueryService.getOperatorAggregate(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), PageRequest.of(0, Integer.MAX_VALUE)
        );

        assertThat(result.entries().content()).hasSize(2);

        OperatorSettlementRes.CreatorSettlementEntry creator2 = result.entries().content().stream()
                .filter(e -> "creator-2".equals(e.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator2.creatorName()).isEqualTo("서강사");
        assertThat(creator2.settleAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOperatorAggregate_endDate가_startDate보다_이전이면_예외() {
        assertThatThrownBy(() -> operatorSettlementQueryService.getOperatorAggregate(
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1), PageRequest.of(0, 20)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.END_DATE_BEFORE_START_DATE);
    }
}
