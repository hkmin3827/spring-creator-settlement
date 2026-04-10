package liveclass.creator_settlement.global.component;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator {
    private final AtomicLong saleRecordCounter = new AtomicLong(10);
    private final AtomicLong courseCounter = new AtomicLong(10);
    private final AtomicLong creatorCounter = new AtomicLong(10);
    private final AtomicLong settlementCounter = new AtomicLong(10);
    private final AtomicLong studentCounter = new AtomicLong(10);

    public String generateSaleRecordId() {
        return "sale-" + saleRecordCounter.getAndIncrement();
    }
    public String generateCourseId() {
        return "course-" + courseCounter.getAndIncrement();
    }
    public String generateCreatorId() {
        return "creator-" + creatorCounter.getAndIncrement();
    }
    public String generateSettlementId() {
        return "settlement-" + settlementCounter.getAndIncrement();
    }
    public String generateCancelRecordId() {
        return "cancel-" + saleRecordCounter.getAndIncrement();
    }
}
