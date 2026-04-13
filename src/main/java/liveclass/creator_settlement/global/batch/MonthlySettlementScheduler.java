package liveclass.creator_settlement.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySettlementScheduler {

    private final JobOperator jobOperator;
    private final Job settlementCreateJob;
    private final Job monthlySettlementConfirmJob;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void runSettlementCreate() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("월별 정산 생성 배치 시작 - 대상 월: {}", previousMonth);

        JobParameters params = new JobParametersBuilder()
                .addString("yearMonth", previousMonth.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobOperator.start(settlementCreateJob, params);
            log.info("월별 정산 생성 배치 완료 - 대상 월: {}", previousMonth);
        } catch (Exception e) {
            log.error("월별 정산 생성 배치 실패 - 대상 월: {}", previousMonth, e);
        }
    }

    @Scheduled(cron = "0 5 0 15 * *", zone = "Asia/Seoul")
    public void runSettlementConfirm() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("월별 정산 확정 배치 시작 - 대상 월: {}", previousMonth);

        JobParameters params = new JobParametersBuilder()
                .addString("yearMonth", previousMonth.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        try {
            jobOperator.start(monthlySettlementConfirmJob, params);
            log.info("월별 정산 확정 배치 완료 - 대상 월: {}", previousMonth);
        } catch (Exception e) {
            log.error("월별 정산 확정 배치 실패 - 대상 월: {}", previousMonth, e);
        }
    }
}
