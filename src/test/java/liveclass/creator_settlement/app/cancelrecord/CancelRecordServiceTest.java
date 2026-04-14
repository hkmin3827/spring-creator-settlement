package liveclass.creator_settlement.app.cancelrecord;

import liveclass.creator_settlement.app.cancelRecord.CancelRecordService;
import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordCreateReq;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.course.CourseRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelRecordServiceTest {

    @Mock CancelRecordRepository cancelRecordRepository;
    @Mock SaleRecordRepository saleRecordRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock CourseRepository courseRepository;
    @Mock IdGenerator idGenerator;
    @InjectMocks CancelRecordService cancelRecordService;

    private LocalDateTime paidAtThisMonth;
    private LocalDateTime paidAtPrevMonthLastDay;
    private YearMonth prevYearMonth;

    private SaleRecord sr1, sr2, sr3;
    private Course cs1, cs2;
    private Settlement sm1;

    @BeforeEach
    void setup() {
        paidAtThisMonth = LocalDateTime.now().minusDays(5);
        paidAtPrevMonthLastDay = LocalDateTime.now().withDayOfMonth(1).minusDays(1)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        prevYearMonth = YearMonth.now().minusMonths(1);

        cs1 = Course.of("course-1", "creator-1", "자바 강의", new BigDecimal("100000"));
        cs2 = Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("60000"));

        sr1 = SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), paidAtThisMonth);
        sr2 = SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("100000"), paidAtThisMonth);
        sr3 = SaleRecord.of("sale-3", "course-2", "student-3", new BigDecimal("60000"), paidAtPrevMonthLastDay);

        sm1 = Settlement.create(
                "settle-1", "creator-2", prevYearMonth.toString(),
                new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("200000"),
                new BigDecimal("0.20"), new BigDecimal("40000"), new BigDecimal("160000"),
                5L, 0L
        );
        sm1.confirm();

        lenient().when(saleRecordRepository.findByIdWithPessimisticLock("sale-1")).thenReturn(Optional.of(sr1));
        lenient().when(courseRepository.findById("course-1")).thenReturn(Optional.of(cs1));
        lenient().when(settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-2", prevYearMonth.toString()))
                .thenReturn(Optional.of(sm1));
    }

    @Test
    @DisplayName("성공 - 동월 판매내역 취소 정상 처리")
    void register_동월_판매내역_취소_정상_처리() {
        CancelRecord cr = CancelRecord.of("cancel-10", "sale-1", paidAtThisMonth, new BigDecimal("50000"), LocalDateTime.now());

        given(idGenerator.generateCancelRecordId()).willReturn("cancel-10");
        given(cancelRecordRepository.save(any())).willReturn(cr);

        var result = cancelRecordService.register(
                new CancelRecordCreateReq("sale-1", new BigDecimal("50000"), LocalDateTime.now())
        );

        assertThat(result.saleRecordId()).isEqualTo("sale-1");
        assertThat(result.id()).isEqualTo("cancel-10");
        assertThat(sr1.status).isEqualTo(SaleRecordStatus.CANCELLED);
        verify(cancelRecordRepository).save(any(CancelRecord.class));
    }

    @Test
    @DisplayName("실패 - 이미 취소된 판매내역 중복 취소 시 예외")
    void register_이미_취소된_판매내역이면_예외() {
        sr1.cancel();

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-1", new BigDecimal("100000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 판매내역 취소 시 예외")
    void register_판매내역이_없으면_예외() {
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-999", new BigDecimal("100000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SALE_RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("실패 - 판매내역에 연결된 강의가 없으면 예외")
    void register_강의를_찾을_수_없으면_예외() {
        SaleRecord sr = SaleRecord.of("sale-1", "course-999", "student-1", new BigDecimal("100000"), paidAtThisMonth);
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-1")).willReturn(Optional.of(sr));
        given(courseRepository.findById("course-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-1", new BigDecimal("100000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("실패 - 결제일로부터 15일 초과 시 취소 기한 만료 예외")
    void register_판매_취소_기한_만료() {
        SaleRecord expiredSr = SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"),
                LocalDateTime.now().minusDays(16));
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-1")).willReturn(Optional.of(expiredSr));

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-1", new BigDecimal("100000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRED_CANCEL_PARIOD);
    }

    @Test
    @DisplayName("성공 - 전월 판매내역을 15일 이내 취소 시 해당 월 정산 환불액 반영")
    void register_과거월_판매_취소_CONFIRMED_정산_환불액_반영() {
        assumeTrue(
            !paidAtPrevMonthLastDay.isBefore(LocalDateTime.now().minusDays(15)),
            "전달 마지막 날이 15일 이내인 달 초(1~15일)에만 실행"
        );

        CancelRecord cancelRecord = CancelRecord.of("cancel-10", "sale-3", sr3.paidAt, new BigDecimal("60000"), LocalDateTime.now());

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-3")).willReturn(Optional.of(sr3));
        given(courseRepository.findById("course-2")).willReturn(Optional.of(cs2));
        given(idGenerator.generateCancelRecordId()).willReturn("cancel-10");
        given(cancelRecordRepository.save(any())).willReturn(cancelRecord);

        cancelRecordService.register(new CancelRecordCreateReq("sale-3", new BigDecimal("60000"), LocalDateTime.now()));

        assertThat(sm1.refundAmount).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(sm1.netAmount).isEqualByComparingTo(new BigDecimal("140000"));
    }

    @Test
    @DisplayName("실패 - 전월 판매내역 취소 시 정산이 이미 PAID 상태이면 예외")
    void register_과거월_판매_취소시_이미_PAID된_정산이면_예외() {
        assumeTrue(
            !paidAtPrevMonthLastDay.isBefore(LocalDateTime.now().minusDays(15)),
            "전달 마지막 날이 15일 이내인 달 초(1~15일)에만 실행"
        );

        sm1.markAsPaid();

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-3")).willReturn(Optional.of(sr3));
        given(courseRepository.findById("course-2")).willReturn(Optional.of(cs2));

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-3", new BigDecimal("60000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_PAID_SETTLEMENT);
    }

    @Test
    @DisplayName("실패 - 전월 판매내역 취소 시 정산이 없으면 예외")
    void register_과거월_판매_취소시_정산이_없으면_예외() {
        assumeTrue(
            !paidAtPrevMonthLastDay.isBefore(LocalDateTime.now().minusDays(15)),
            "전달 마지막 날이 15일 이내인 달 초(1~15일)에만 실행"
        );

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-3")).willReturn(Optional.of(sr3));
        given(courseRepository.findById("course-2")).willReturn(Optional.of(cs2));
        given(settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-2", prevYearMonth.toString()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> cancelRecordService.register(
                new CancelRecordCreateReq("sale-3", new BigDecimal("60000"), LocalDateTime.now())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("성공 - 전월 취소 후 정산 금액이 수수료 포함하여 올바르게 재계산됨")
    void register_과거월_취소_후_정산_금액이_올바르게_재계산됨() {
        assumeTrue(
            !paidAtPrevMonthLastDay.isBefore(LocalDateTime.now().minusDays(15)),
            "전달 마지막 날이 15일 이내인 달 초(1~15일)에만 실행"
        );

        Settlement settlement = Settlement.create(
                "settle-2", "creator-2", prevYearMonth.toString(),
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("300000"),
                new BigDecimal("0.20"), new BigDecimal("60000"), new BigDecimal("240000"),
                3L, 0L
        );
        settlement.confirm();

        CancelRecord cancelRecord = CancelRecord.of("cancel-10", "sale-3", sr3.paidAt, new BigDecimal("60000"), LocalDateTime.now());

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-3")).willReturn(Optional.of(sr3));
        given(courseRepository.findById("course-2")).willReturn(Optional.of(cs2));
        given(settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-2", prevYearMonth.toString()))
                .willReturn(Optional.of(settlement));
        given(idGenerator.generateCancelRecordId()).willReturn("cancel-10");
        given(cancelRecordRepository.save(any())).willReturn(cancelRecord);

        cancelRecordService.register(new CancelRecordCreateReq("sale-3", new BigDecimal("60000"), LocalDateTime.now()));

        assertThat(settlement.refundAmount).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(settlement.netAmount).isEqualByComparingTo(new BigDecimal("240000"));
        assertThat(settlement.commissionAmount).isEqualByComparingTo(new BigDecimal("48000.0000"));
        assertThat(settlement.settleAmount).isEqualByComparingTo(new BigDecimal("192000.0000"));
    }

    @Test
    @DisplayName("성공 - 동월 판매내역 부분 환불 시 취소 처리 정상")
    void register_부분환불_취소처리_정상() {
        SaleRecord sr4 = SaleRecord.of("sale-4", "course-1", "student-4", new BigDecimal("80000"), paidAtThisMonth);
        CancelRecord cr = CancelRecord.of("cancel-10", "sale-4", paidAtThisMonth, new BigDecimal("30000"), LocalDateTime.now());

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-4")).willReturn(Optional.of(sr4));
        given(courseRepository.findById("course-1")).willReturn(Optional.of(cs1));
        given(idGenerator.generateCancelRecordId()).willReturn("cancel-10");
        given(cancelRecordRepository.save(any())).willReturn(cr);

        var result = cancelRecordService.register(
                new CancelRecordCreateReq("sale-4", new BigDecimal("30000"), LocalDateTime.now())
        );

        assertThat(result.saleRecordId()).isEqualTo("sale-4");
        assertThat(sr4.status).isEqualTo(SaleRecordStatus.CANCELLED);
        verify(cancelRecordRepository).save(any(CancelRecord.class));
    }

    @Test
    @DisplayName("성공 - 동월 다수 취소 시 각각 독립적으로 처리")
    void register_동월에_다수_취소_발생시_각각_독립적으로_처리() {
        SaleRecord sr4 = SaleRecord.of("sale-4", "course-2", "student-4", new BigDecimal("60000"), paidAtPrevMonthLastDay);
        SaleRecord sr5 = SaleRecord.of("sale-5", "course-2", "student-5", new BigDecimal("60000"), paidAtPrevMonthLastDay);

        CancelRecord cr1 = CancelRecord.of("cancel-10", "sale-1", paidAtThisMonth, new BigDecimal("100000"), LocalDateTime.now());
        CancelRecord cr2 = CancelRecord.of("cancel-11", "sale-2", paidAtThisMonth, new BigDecimal("100000"), LocalDateTime.now());
        CancelRecord cr3 = CancelRecord.of("cancel-12", "sale-3", paidAtThisMonth, new BigDecimal("100000"), LocalDateTime.now());
        CancelRecord cr4 = CancelRecord.of("cancel-13", "sale-4", paidAtThisMonth, new BigDecimal("100000"), LocalDateTime.now());
        CancelRecord cr5 = CancelRecord.of("cancel-14", "sale-5", paidAtThisMonth, new BigDecimal("100000"), LocalDateTime.now());

        given(saleRecordRepository.findByIdWithPessimisticLock("sale-2")).willReturn(Optional.of(sr2));
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-3")).willReturn(Optional.of(sr3));
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-4")).willReturn(Optional.of(sr4));
        given(saleRecordRepository.findByIdWithPessimisticLock("sale-5")).willReturn(Optional.of(sr5));
        given(courseRepository.findById("course-2")).willReturn(Optional.of(cs2));
        given(idGenerator.generateCancelRecordId()).willReturn("cancel-10").willReturn("cancel-11").willReturn("cancel-12").willReturn("cancel-13").willReturn("cancel-14");
        given(cancelRecordRepository.save(any())).willReturn(cr1).willReturn(cr2).willReturn(cr3).willReturn(cr4).willReturn(cr5);

        var result1 = cancelRecordService.register(new CancelRecordCreateReq("sale-1", new BigDecimal("100000"), LocalDateTime.now()));
        var result2 = cancelRecordService.register(new CancelRecordCreateReq("sale-2", new BigDecimal("100000"), LocalDateTime.now()));
        var result3 = cancelRecordService.register(new CancelRecordCreateReq("sale-3", new BigDecimal("100000"), LocalDateTime.now()));
        var result4 = cancelRecordService.register(new CancelRecordCreateReq("sale-4", new BigDecimal("100000"), LocalDateTime.now()));
        var result5 = cancelRecordService.register(new CancelRecordCreateReq("sale-5", new BigDecimal("100000"), LocalDateTime.now()));

        assertThat(result1.saleRecordId()).isEqualTo("sale-1");
        assertThat(result2.saleRecordId()).isEqualTo("sale-2");
        assertThat(result3.saleRecordId()).isEqualTo("sale-3");
        assertThat(result4.saleRecordId()).isEqualTo("sale-4");
        assertThat(result5.saleRecordId()).isEqualTo("sale-5");
    }
}
