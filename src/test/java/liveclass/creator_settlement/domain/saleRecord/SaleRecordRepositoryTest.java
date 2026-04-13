package liveclass.creator_settlement.domain.saleRecord;

import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.creator.Creator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SaleRecordRepositoryTest {

    @Autowired
    TestEntityManager em;
    @Autowired SaleRecordRepository saleRecordRepository;

    @BeforeEach
    void setUp() {
        em.persist(Creator.of("creator-1", "홍길동"));
        em.persist(Course.of("course-1", "creator-1", "Java 강의", new BigDecimal("100000")));
        em.flush();
    }

    @Test
    void findByCreatorIdAndPaidAtBetween_범위_내_판매내역_반환() {
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.persist(SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("50000"), null));
        em.flush();

        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                "creator-1",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5),
                Pageable.unpaged()
        ).getContent();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(s -> s.courseId).containsOnly("course-1");
    }

    @Test
    void findByCreatorIdAndPaidAtBetween_다른_크리에이터_제외() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.persist(SaleRecord.of("sale-2", "course-2", "student-2", new BigDecimal("50000"), null));
        em.flush();

        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                "creator-1",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5),
                Pageable.unpaged()
        ).getContent();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo("sale-1");
    }

    @Test
    void findByCreatorIdAndPaidAtBetween_범위_외_데이터_제외() {
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.flush();

        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                "creator-1",
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59),
                Pageable.unpaged()
        ).getContent();

        assertThat(result).isEmpty();
    }

    @Test
    void aggregateSalesByCreatorInRange_크리에이터별_금액_합산_및_건수() {
        em.persist(Creator.of("creator-2", "김철수"));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.persist(SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("200000"), null));
        em.persist(SaleRecord.of("sale-3", "course-2", "student-3", new BigDecimal("50000"), null));
        em.flush();

        List<CreatorAggregationDto> result = saleRecordRepository.aggregateSalesByCreatorInRange(
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(5)
        );

        assertThat(result).hasSize(2);

        CreatorAggregationDto creator1 = result.stream()
                .filter(r -> "creator-1".equals(r.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator1.totalAmount()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(creator1.count()).isEqualTo(2L);
    }

    @Test
    void aggregateSalesByCreatorInRange_범위_외_데이터_제외() {
        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), null));
        em.flush();

        List<CreatorAggregationDto> result = saleRecordRepository.aggregateSalesByCreatorInRange(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59)
        );

        assertThat(result).isEmpty();
    }
}
