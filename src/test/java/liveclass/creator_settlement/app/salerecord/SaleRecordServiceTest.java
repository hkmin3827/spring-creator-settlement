package liveclass.creator_settlement.app.salerecord;

import liveclass.creator_settlement.app.saleRecord.SaleRecordService;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordCreateReq;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.course.CourseRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaleRecordServiceTest {

    @Mock SaleRecordRepository saleRecordRepository;
    @Mock CourseRepository courseRepository;
    @Mock IdGenerator idGenerator;
    @InjectMocks SaleRecordService saleRecordService;

    private Course course;
    private SaleRecord saleRecord;

    @BeforeEach
    void setup() {
        course = Course.of("course-1", "creator-1", "자바 강의", new BigDecimal("100000"));
        saleRecord = SaleRecord.of("sale-10", "course-1", "student-1", new BigDecimal("100000"), LocalDateTime.of(2025, 3, 15, 10, 0));

        lenient().when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        lenient().when(idGenerator.generateSaleRecordId()).thenReturn("sale-10");
        lenient().when(saleRecordRepository.save(any())).thenReturn(saleRecord);
    }

    @Test
    void register_정상_판매내역_저장() {
        String result = saleRecordService.register(
                new SaleRecordCreateReq("course-1", "student-1", new BigDecimal("100000"), LocalDateTime.of(2025, 3, 15, 10, 0))
        );

        assertThat(result).isEqualTo("sale-10");
        verify(saleRecordRepository).save(any(SaleRecord.class));
    }

    @Test
    void register_강의_가격보다_판매금액이_크면_예외() {
        assertThatThrownBy(() -> saleRecordService.register(
                new SaleRecordCreateReq("course-1", "student-1", new BigDecimal("150000"), LocalDateTime.of(2025, 3, 15, 10, 0))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SALE_RECORD_AMOUNT);
    }

    @Test
    void register_강의를_찾을_수_없으면_예외() {
        given(courseRepository.findById("course-999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> saleRecordService.register(
                new SaleRecordCreateReq("course-999", "student-1", new BigDecimal("100000"), LocalDateTime.of(2025, 3, 15, 10, 0))
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void getSaleRecords_시작일_종료일_모두_있으면_범위_조회() {
        PageRequest pageable = PageRequest.of(0, 10);

        given(saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                eq("creator-1"), any(), any(), eq(pageable)
        )).willReturn(new PageImpl<>(List.of(saleRecord)));

        Page<?> result = saleRecordService.getSaleRecords("creator-1",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSaleRecords_시작일만_있으면_시작일_이후_조회() {
        PageRequest pageable = PageRequest.of(0, 10);

        given(saleRecordRepository.findByCreatorIdAndPaidAtStart(
                eq("creator-1"), any(), eq(pageable)
        )).willReturn(new PageImpl<>(List.of(saleRecord)));

        Page<?> result = saleRecordService.getSaleRecords("creator-1",
                LocalDate.of(2025, 3, 1), null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSaleRecords_종료일만_있으면_종료일_이전_조회() {
        PageRequest pageable = PageRequest.of(0, 10);

        given(saleRecordRepository.findByCreatorIdAndPaidAtEnd(
                eq("creator-1"), any(), eq(pageable)
        )).willReturn(new PageImpl<>(List.of(saleRecord)));

        Page<?> result = saleRecordService.getSaleRecords("creator-1",
                null, LocalDate.of(2025, 3, 31), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSaleRecords_날짜_없으면_전체_조회() {
        SaleRecord r = SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), LocalDateTime.of(2025, 1, 5, 10, 0));
        PageRequest pageable = PageRequest.of(0, 10);

        given(saleRecordRepository.findAllByCreatorId(eq("creator-1"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(saleRecord, r)));

        Page<?> result = saleRecordService.getSaleRecords("creator-1", null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
    }
}
