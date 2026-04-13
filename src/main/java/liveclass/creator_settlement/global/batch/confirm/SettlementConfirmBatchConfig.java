package liveclass.creator_settlement.global.batch.confirm;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SettlementConfirmBatchConfig {

    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job monthlySettlementConfirmJob(Step monthlySettlementConfirmStep) {
        return new JobBuilder("monthlySettlementConfirmJob", jobRepository)
                .start(monthlySettlementConfirmStep)
                .build();
    }

    @Bean
    public Step monthlySettlementConfirmStep(
            JpaPagingItemReader<Settlement> confirmSettlementItemReader,
            SettlementConfirmItemProcessor confirmProcessor,
            SettlementConfirmItemWriter confirmWriter,
            SettlementConfirmSkipListener confirmSkipListener) {
        return new StepBuilder("monthlySettlementConfirmStep", jobRepository)
                .<Settlement, Settlement>chunk(50)
                .reader(confirmSettlementItemReader)
                .processor(confirmProcessor)
                .writer(confirmWriter)
                .faultTolerant()
                .skip(BusinessException.class)
                .skipLimit(10)
                .retry(DataAccessResourceFailureException.class)
                .retry(PessimisticLockingFailureException.class)
                .retry(ObjectOptimisticLockingFailureException.class)
                .retry(OptimisticLockException.class)
                .retryLimit(2)
                .listener(confirmSkipListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Settlement> confirmSettlementItemReader(
            @Value("#{jobParameters['yearMonth']}") String yearMonth) {
        Map<String, Object> params = new HashMap<>();
        params.put("yearMonth", yearMonth);
        params.put("status", SettlementStatus.PENDING);

        return new JpaPagingItemReaderBuilder<Settlement>()
                .name("confirmSettlementItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s FROM Settlement s WHERE s.status = :status AND s.yearMonth = :yearMonth ORDER BY s.id")
                .parameterValues(params)
                .pageSize(50)
                .build();
    }
}
