package liveclass.creator_settlement.global.batch.create;

import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.global.batch.dto.SettlementBatchItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SettlementCreateItemWriter implements ItemWriter<SettlementBatchItem> {

    private final SettlementService settlementService;

    @Override
    public void write(Chunk<? extends SettlementBatchItem> chunk) {
        for (SettlementBatchItem item : chunk) {
            settlementService.createPending(item.creatorId(), YearMonth.parse(item.yearMonth()));
            log.info("정산 생성 완료 - creatorId: {}, yearMonth: {}", item.creatorId(), item.yearMonth());
        }
    }
}
