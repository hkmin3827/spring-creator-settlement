package liveclass.creator_settlement.domain.saleRecord;

import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.creator.Creator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SaleRecordRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    private SaleRecordRepository saleRecordRepository;

    @Autowired
    private PlatformTransactionManager ptm;

    private TransactionTemplate tt;


    @BeforeEach
    void setUp() {
        em.persist(Creator.of("creator-1", "유강사"));
        em.persist(Creator.of("creator-2", "김철수"));

        em.persist(Course.of("course-1", "creator-1", "Java 강의", new BigDecimal("100000")));
        em.persist(Course.of("course-2", "creator-2", "Python 강의", new BigDecimal("50000")));


        em.persist(SaleRecord.of("sale-1", "course-1", "student-1", new BigDecimal("100000"), LocalDateTime.parse("2025-03-27T13:00:00")));
        em.persist(SaleRecord.of("sale-2", "course-1", "student-2", new BigDecimal("100000"), LocalDateTime.parse("2025-03-17T13:00:00")));
        em.persist(SaleRecord.of("sale-3", "course-2", "student-4", new BigDecimal("50000"), LocalDateTime.parse("2025-03-27T13:00:00")));
        em.persist(SaleRecord.of("sale-5", "course-2", "student-10", new BigDecimal("50000"), LocalDateTime.parse("2025-06-17T13:00:00")));
        em.persist(SaleRecord.of("sale-6", "course-1", "student-11", new BigDecimal("100000"), LocalDateTime.parse("2026-03-17T13:00:00")));

        em.flush();
        em.clear();

        tt = new TransactionTemplate(ptm);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            em.persist(SaleRecord.of("sale-10", "course-2", "student-3", new BigDecimal("50000"), LocalDateTime.parse("2026-03-10T13:00:00")));
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        tt.execute(status -> {
            em.getEntityManager().createQuery("DELETE FROM SaleRecord").executeUpdate();
            return null;
        });
    }

    @Test
    @DisplayName("findByIdWithPessimisticLock - 성공")
    void findByIdWithPessimisticLock_성공() {
        Optional<SaleRecord> opt1 = saleRecordRepository.findByIdWithPessimisticLock("sale-3");

        assertThat(opt1).isPresent();
        assertThat(opt1.get().id).isEqualTo("sale-3");
        assertThat(opt1.get().amount).isEqualTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("findByIdWithPessimisticLock - 잠금 획득 실패 (타임아웃 또는 동시 접근)")
    void findByIdWithPessimisticLock_실패_잠금_획득_실패() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch thread2FinishedLatch = new CountDownLatch(1);
        AtomicReference<Exception> threadException = new AtomicReference<>();

        executorService.submit(() -> {
            tt.execute(status -> {
                saleRecordRepository.findByIdWithPessimisticLock("sale-10");
                lockAcquiredLatch.countDown();
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        });

        executorService.submit(() -> {
            try {
                lockAcquiredLatch.await(2, TimeUnit.SECONDS);
                tt.execute(status -> {
                    saleRecordRepository.findByIdWithPessimisticLock("sale-10");
                    return null;
                });
            } catch (Exception e) {
                threadException.set(e);
            } finally {
                thread2FinishedLatch.countDown();
            }
        });

        thread2FinishedLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(threadException.get())
                .isNotNull()
                .isInstanceOf(DataAccessException.class);
    }


    @Test
    @DisplayName("성공 - findByCreatorIdAndPaidAtBetween")
    void findByCreatorIdAndPaidAtBetween_성공_범위_내_판매내역_반환() {
        LocalDateTime start = LocalDateTime.of(2025, 3, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 3, 31, 23, 59);

        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                "creator-1",
                start,
                end,
                Pageable.unpaged()
        ).getContent();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(s -> s.courseId).containsOnly("course-1");
    }

    @Test
    @DisplayName("성공 - findByCreatorIdAndPaidAtBetween : 기간에 데이터 없을 시 0 반환")
    void findByCreatorIdAndPaidAtBetween_성공_범위_외_데이터_제외() {
        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                "creator-1",
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59),
                Pageable.unpaged()
        ).getContent();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공 - findByCreatorIdAndPaidAtStart")
    void findByCreatorIdAndPaidAtStart_성공_범위_내_판매내역_반환() {
        LocalDateTime start = LocalDateTime.parse("2020-01-01T00:00:00");

        List<SaleRecord> result = saleRecordRepository.findByCreatorIdAndPaidAtStart(
                "creator-1",
                start,
                Pageable.unpaged()
        ).getContent();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(s -> s.courseId).containsOnly("course-1");
    }


    @Test
    @DisplayName("성공 - findAllByCreatorId")
    void findAllByCreatorId_성공_모든_판매_내역_반환() {
        List<SaleRecord> result = saleRecordRepository.findAllByCreatorId("creator-2", Pageable.unpaged()).getContent();

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("성공 - aggregateSalesByCreatorInRange")
    void aggregateSalesByCreatorInRange_크리에이터별_금액_합산_및_건수() {
        List<CreatorAggregationDto> result = saleRecordRepository.aggregateSalesByCreatorInRange(
                LocalDateTime.parse("2025-03-01T00:00:00"),
                LocalDateTime.parse("2025-03-31T11:59:59")
        );

        assertThat(result).hasSize(2);

        CreatorAggregationDto creator1 = result.stream()
                .filter(r -> "creator-1".equals(r.creatorId()))
                .findFirst().orElseThrow();

        assertThat(creator1.amount()).isEqualByComparingTo(new BigDecimal("200000"));
    }

    @Test
    @DisplayName("성공 - aggregateSalesByCreatorInRange : 기간 내 데이터 없음. 0개 반환")
    void aggregateSalesByCreatorInRange_범위_외_데이터_제외() {
        List<CreatorAggregationDto> result = saleRecordRepository.aggregateSalesByCreatorInRange(
                LocalDateTime.of(2020, 1, 1, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59)
        );

        assertThat(result).isEmpty();
    }
}
