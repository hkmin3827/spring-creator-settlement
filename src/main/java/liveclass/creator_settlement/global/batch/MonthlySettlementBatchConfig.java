package liveclass.creator_settlement.global.batch;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import liveclass.creator_settlement.app.settlement.dto.SettlementBatchItem;
import liveclass.creator_settlement.domain.creator.Creator;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MonthlySettlementBatchConfig {

    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job monthlySettlementJob(Step monthlySettlementStep) {
        return new JobBuilder("monthlySettlementJob", jobRepository)
                .start(monthlySettlementStep)
                .build();
    }

    @Bean
    public Step monthlySettlementStep(
            JpaPagingItemReader<Creator> creatorItemReader,
            SettlementItemProcessor processor,
            SettlementItemWriter writer,
            SettlementSkipListener skipListener) {
        return new StepBuilder("monthlySettlementStep", jobRepository)
                .<Creator, SettlementBatchItem>chunk(50)
                .reader(creatorItemReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(BusinessException.class)
                .skipLimit(10)
                .retry(DataAccessResourceFailureException.class)
                .retry(PessimisticLockingFailureException.class)
                .retry(ObjectOptimisticLockingFailureException.class)
                .retry(OptimisticLockException.class)
                .retryLimit(2)
                .listener(skipListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Creator> creatorItemReader() {
        return new JpaPagingItemReaderBuilder<Creator>()
                .name("creatorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM Creator c ORDER BY c.id")
                .pageSize(50)
                .build();
    }
}
