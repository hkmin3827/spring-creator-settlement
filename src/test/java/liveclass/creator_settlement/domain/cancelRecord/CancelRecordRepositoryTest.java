package liveclass.creator_settlement.domain.cancelRecord;

import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CancelRecordRepositoryTest {

    @Autowired
    TestEntityManager em;
    @Autowired CancelRecordRepository cancelRecordRepository;

    @BeforeEach
    void setUp() {
        em.persist(Creator.of("creator-5", "민강사"));
        em.persist(Course.of("course-9", "creator-5", "Java 강의", new BigDecimal("80000")));
        em.persist(SaleRecord.of("sale-1", "course-9", "student-1", new BigDecimal("80000"),LocalDateTime.parse("2026-04-10T14:30:00")));
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2026-04-10T14:30:00"), new BigDecimal("40000"), LocalDateTime.parse("2026-04-15T15:30:00")));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("성공 - findByCreatorIdAndCancelledAtBetween")
    void findByCreatorIdAndCancelledAtBetween_범위_내_취소내역_반환() {
        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-5",
                LocalDateTime.parse("2026-04-01T00:00:00"),
                LocalDateTime.parse("2026-04-20T00:00:00")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).refundAmount).isEqualByComparingTo(new BigDecimal("40000"));
    }

    @Test
    @DisplayName("성공 - findByCreatorIdAndCancelledAtBetween : 다른 크리에이터 취소 건 제외")
    void findByCreatorIdAndCancelledAtBetween_다른_크리에이터_제외() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-2", "course-2", "student-2", new BigDecimal("50000"), LocalDateTime.parse("2026-04-10T14:30:00")));
        em.persist(SaleRecord.of("sale-3", "course-2", "student-5", new BigDecimal("50000"), LocalDateTime.parse("2026-04-10T14:30:00")));
        em.persist(CancelRecord.of("cancel-2", "sale-2", LocalDateTime.parse("2026-04-13T14:30:00"), new BigDecimal("100000"), LocalDateTime.parse("2026-04-15T15:30:00")));
        em.persist(CancelRecord.of("cancel-3", "sale-3", LocalDateTime.parse("2026-04-12T14:30:00"), new BigDecimal("50000"), LocalDateTime.parse("2026-04-15T15:30:00")));
        em.flush();

        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-5",
                LocalDateTime.parse("2026-04-01T00:00:00"),
                LocalDateTime.parse("2026-04-20T00:00:00")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo("cancel-1");
    }

    @Test
    void findByCreatorIdAndCancelledAtBetween_범위_외_데이터_제외() {
        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-5",
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void aggregateCancelsByCreatorInRange_크리에이터별_취소금액_집계() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-4", "course-2", "student-2", new BigDecimal("50000"), LocalDateTime.parse("2026-04-05T14:30:00")));
        em.persist(SaleRecord.of("sale-5", "course-2", "student-5", new BigDecimal("50000"), LocalDateTime.parse("2026-04-05T14:30:00")));
        em.persist(SaleRecord.of("sale-6", "course-2", "student-7", new BigDecimal("50000"), LocalDateTime.parse("2026-04-05T14:30:00")));
        em.persist(CancelRecord.of("cancel-4", "sale-4", LocalDateTime.parse("2026-04-05T14:30:00"),new BigDecimal("50000"), LocalDateTime.parse("2026-04-15T14:30:00")));
        em.persist(CancelRecord.of("cancel-5", "sale-5", LocalDateTime.parse("2026-04-05T14:30:00"),new BigDecimal("30000"), LocalDateTime.parse("2026-04-18T14:30:00")));
        em.flush();

        List<CreatorAggregationDto> result = cancelRecordRepository.aggregateCancelsByCreatorInRange(
                LocalDateTime.parse("2026-04-01T00:00:00"),
                LocalDateTime.parse("2026-04-20T00:00:00")
        );

        assertThat(result).hasSize(2);

        CreatorAggregationDto creator1 = result.stream()
                .filter(r -> "creator-2".equals(r.creatorId()))
                .findFirst().orElseThrow();

        CreatorAggregationDto creator2 = result.stream()
                .filter(r -> "creator-5".equals(r.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator1.amount()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(creator2.amount()).isEqualByComparingTo(new BigDecimal("40000"));
    }
}
