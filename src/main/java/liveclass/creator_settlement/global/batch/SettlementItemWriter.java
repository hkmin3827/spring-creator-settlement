package liveclass.creator_settlement.global.batch;

import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.app.settlement.dto.SettlementBatchItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SettlementItemWriter implements ItemWriter<SettlementBatchItem> {

    private final SettlementService settlementService;

    @Override
    public void write(Chunk<? extends SettlementBatchItem> chunk) {
        for (SettlementBatchItem item : chunk) {
            String settlementId = settlementService.createPending(item.creatorId(), item.yearMonth());
            settlementService.confirmPending(settlementId);
            log.info("정산 확정 완료 - creatorId: {}, yearMonth: {}", item.creatorId(), item.yearMonth());
        }
    }
}
