package liveclass.creator_settlement.global.batch.create;

import liveclass.creator_settlement.app.settlement.dto.SettlementBatchItem;
import liveclass.creator_settlement.domain.creator.Creator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettlementCreateSkipListener implements SkipListener<Creator, SettlementBatchItem> {

    @Override
    public void onSkipInWrite(SettlementBatchItem item, Throwable t) {
        log.error("[생성 배치 최종 실패 !!!] 관리자 수동 후처리 필요 - creatorId: {}, yearMonth: {}, error: {}",
                item.creatorId(), item.yearMonth(), t.getMessage());
    }

    @Override
    public void onSkipInProcess(Creator item, Throwable t) {
        log.error("[생성 배치 최종 실패 !!!] processor 처리 실패 - creatorId: {}, error: {}",
                item.id, t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("[배치 읽기 실패] 크리에이터 조회 실패 - error: {}", t.getMessage());
    }
}
