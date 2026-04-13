package liveclass.creator_settlement.domain.cancelRecord;

import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import org.junit.jupiter.api.BeforeEach;
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
        em.persist(Creator.of("creator-1", "홍길동"));
        em.persist(Course.of("course-1", "creator-1", "Java 강의", new BigDecimal("100000")));
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.flush();
    }

    @Test
    void findByCreatorIdAndCancelledAtBetween_범위_내_취소내역_반환() {
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2025-03-15T14:30:00"), new BigDecimal("100000"), null));
        em.flush();

        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-1",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).refundAmount).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void findByCreatorIdAndCancelledAtBetween_다른_크리에이터_제외() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-2", "course-2", "student-2", new BigDecimal("50000"), null));
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2025-03-15T14:30:00"), new BigDecimal("100000"), null));
        em.persist(CancelRecord.of("cancel-2", "sale-2", LocalDateTime.parse("2025-03-15T14:30:00"), new BigDecimal("50000"), null));
        em.flush();

        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-1",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo("cancel-1");
    }

    @Test
    void findByCreatorIdAndCancelledAtBetween_범위_외_데이터_제외() {
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2025-03-15T14:30:00"), new BigDecimal("100000"), null));
        em.flush();

        List<CancelRecord> result = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(
                "creator-1",
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void aggregateCancelsByCreatorInRange_크리에이터별_취소금액_집계() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-2", "course-2", "student-2", new BigDecimal("50000"), null));
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2025-03-15T14:30:00"),new BigDecimal("100000"), null));
        em.persist(CancelRecord.of("cancel-2", "sale-2", LocalDateTime.parse("2025-03-15T14:30:00"),new BigDecimal("50000"), null));
        em.flush();

        List<CreatorAggregationDto> result = cancelRecordRepository.aggregateCancelsByCreatorInRange(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        );

        assertThat(result).hasSize(2);

        CreatorAggregationDto creator1 = result.stream()
                .filter(r -> "creator-1".equals(r.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator1.totalAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(creator1.count()).isEqualTo(1L);
    }

    @Test
    void aggregateCancelsByCreatorInRange_범위_외_데이터_제외() {
        em.persist(CancelRecord.of("cancel-1", "sale-1", LocalDateTime.parse("2025-03-15T14:30:00"), new BigDecimal("100000"), null));
        em.flush();

        List<CreatorAggregationDto> result = cancelRecordRepository.aggregateCancelsByCreatorInRange(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59)
        );

        assertThat(result).isEmpty();
    }
}
