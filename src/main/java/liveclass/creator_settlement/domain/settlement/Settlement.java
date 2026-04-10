package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "year_month"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {
    @Id
    public String id;

    @Enumerated(EnumType.STRING)
    public SettlementStatus status;

    @Column(name = "creator_id", nullable = false, updatable = false)
    public String creatorId;

    @Column(name = "year_month", nullable = false, length = 7, updatable = false)
    public String yearMonth;

    @CreationTimestamp
    @Column(updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public static Settlement confirm(String id, String creatorId, String yearMonth) {
        Settlement s = new Settlement();
        s.id = id;
        s.creatorId = creatorId;
        s.yearMonth = yearMonth;
        s.status = SettlementStatus.CONFIRMED;
        return s;
    }

    public void markAsPaid() {
        this.status = SettlementStatus.PAID;
    }
}
