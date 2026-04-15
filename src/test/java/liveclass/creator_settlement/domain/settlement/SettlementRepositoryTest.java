package liveclass.creator_settlement.domain.settlement;

import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
public class SettlementRepositoryTest {

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlatformTransactionManager ptm;

    private TransactionTemplate tt;

    @BeforeEach
    void setUp() {
        Creator creator1 = Creator.of("creator-1", "김강사");
        Creator creator2 = Creator.of("creator-2", "이강사");
        Creator creator3 = Creator.of("creator-3", "박강사");
        em.persist(creator1);
        em.persist(creator2);
        em.persist(creator3);

        em.persist(Settlement.create("settlement-1", creator1.id, "2025-12", BigDecimal.valueOf(120000), BigDecimal.valueOf(20000), BigDecimal.valueOf(100000), BigDecimal.valueOf(0.20), BigDecimal.valueOf(20000), BigDecimal.valueOf(80000), 6, 1));
        em.persist(Settlement.create("settlement-2", creator2.id, "2026-03", BigDecimal.valueOf(200000), BigDecimal.valueOf(40000), BigDecimal.valueOf(160000), BigDecimal.valueOf(0.20), BigDecimal.valueOf(32000), BigDecimal.valueOf(128000), 10, 2));
        em.persist(Settlement.create("settlement-3", creator3.id, "2026-03", BigDecimal.valueOf(100000), BigDecimal.valueOf(10000), BigDecimal.valueOf(90000), BigDecimal.valueOf(0.20), BigDecimal.valueOf(18000), BigDecimal.valueOf(72000), 10, 2));
        em.flush();
        em.clear();

        tt = new TransactionTemplate(ptm);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            Creator creator4 = Creator.of("creator-4", "최강사");
            em.getEntityManager().persist(creator4);
            em.getEntityManager().persist(Settlement.create("settlement-4", creator4.id, "2026-04", BigDecimal.valueOf(200000), BigDecimal.valueOf(40000), BigDecimal.valueOf(160000), BigDecimal.valueOf(0.20), BigDecimal.valueOf(32000), BigDecimal.valueOf(128000), 10, 2));
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        tt.execute(status -> {
            em.getEntityManager().createQuery("DELETE FROM Settlement").executeUpdate();
            em.getEntityManager().createQuery("DELETE FROM Creator").executeUpdate();
            return null;
        });
    }

    @Test
    @DisplayName("findByCreatorIdAndYearMonth : 크리에이터 id 와 년월로 조회 - 성공")
    void findByCreatorIdAndYearMonth_성공() {
        Optional<Settlement> opt = settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-12");

        assertThat(opt).isPresent();
        assertThat(opt.get().yearMonth).isEqualTo("2025-12");
        assertThat(opt.get().creatorId).isEqualTo("creator-1");
        assertThat(opt.get().id).isEqualTo("settlement-1");
    }

    @Test
    @DisplayName("findByCreatorIdAndYearMonth - 잘못된 년월 조회 (결과 없음)")
    void findByCreatorIdAndYearMonth_실패_잘못된_년월_조회() {
        Optional<Settlement> opt1 = settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-12");
        Optional<Settlement> opt2 = settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025/12");

        assertThat(opt1).isPresent();
        assertThat(opt2).isEmpty();
    }

    @Test
    @DisplayName("findByCreatorIdAndYearMonthWithPessimisticLock - 성공")
    void findByCreatorIdAndYearMonthWithPessimisticLock_성공() {
        Optional<Settlement> opt1 = settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-3", "2026-03");

        assertThat(opt1).isPresent();
        assertThat(opt1.get().creatorId).isEqualTo("creator-3");
    }

    @Test
    @DisplayName("findByCreatorIdAndYearMonthWithPessimisticLock - 잠금 획득 실패 (타임아웃 또는 동시 접근)")
    void findByCreatorIdAndYearMonthWithPessimisticLock_실패_잠금_획득_실패() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch thread2FinishedLatch = new CountDownLatch(1);
        AtomicReference<Exception> threadException = new AtomicReference<>();

        executorService.submit(() -> {
            tt.execute(status -> {
                settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-4", "2026-04");
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
                    settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock("creator-4", "2026-04");
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
    @DisplayName("bulkUpdateStatus - 성공")
    void bulkUpdateStatus_성공() {
        Settlement s1 = settlementRepository.findById("settlement-1").orElseThrow();
        Settlement s2 = settlementRepository.findById("settlement-2").orElseThrow();
        Settlement s3 = settlementRepository.findById("settlement-3").orElseThrow();
        s1.confirm();
        s2.confirm();
        s3.confirm();

        int updatedCount = settlementRepository.bulkUpdateStatus("2026-03", SettlementStatus.PAID, SettlementStatus.CONFIRMED, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        assertThat(updatedCount).isEqualTo(2);

        List<Settlement> updatedSettlements = settlementRepository.findAll();
        assertThat(updatedSettlements).extracting("status").contains(SettlementStatus.PAID);
    }

    @Test
    @DisplayName("bulkUpdateStatus - 이미 결제된 상태이므로 업데이트 실패 (0건 변경)")
    void bulkUpdateStatus_실패_이미_결제된_정산() {
        Settlement settlement = settlementRepository.findByCreatorIdAndYearMonth("creator-1", "2025-12").orElseThrow();
        settlement.markAsPaid();

        em.flush();
        em.clear();

        int updatedCount = settlementRepository.bulkUpdateStatus("2025-12", SettlementStatus.PAID, SettlementStatus.CONFIRMED, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        assertThat(updatedCount).isEqualTo(0);
    }
}
