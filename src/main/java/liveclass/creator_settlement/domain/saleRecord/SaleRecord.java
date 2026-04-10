package liveclass.creator_settlement.domain.saleRecord;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sale_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleRecord {
    @Id
    public String id;

    public SaleRecordStatus status;

    @Column(name="course_id", nullable = false)
    public String courseId;

    @Column(name="student_id", nullable = false)
    public String studentId;

    public Long amount;

    @CreationTimestamp
    public LocalDateTime paidAt;
}
