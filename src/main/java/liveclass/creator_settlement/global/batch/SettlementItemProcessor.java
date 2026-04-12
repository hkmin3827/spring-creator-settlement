package liveclass.creator_settlement.global.batch;

import liveclass.creator_settlement.app.settlement.dto.SettlementBatchItem;
import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@StepScope
public class SettlementItemProcessor implements ItemProcessor<Creator, SettlementBatchItem> {

    private final String yearMonth;
    private final SettlementRepository settlementRepository;

    public SettlementItemProcessor(
            @Value("#{jobParameters['yearMonth']}") String yearMonth,
            SettlementRepository settlementRepository) {
        this.yearMonth = yearMonth;
        this.settlementRepository = settlementRepository;
    }

    @Override
    public SettlementBatchItem process(Creator creator) {
        if (settlementRepository.existsByCreatorIdAndYearMonthAndStatusIn(
                creator.id, yearMonth,
                List.of(SettlementStatus.CONFIRMED, SettlementStatus.PAID))) {
            log.info("***이미 존재하는 정산 - 스킵*** - creatorId: {}, yearMonth: {}", creator.id, yearMonth);
            return null;
        }
        return new SettlementBatchItem(creator.id, yearMonth);
    }
}
