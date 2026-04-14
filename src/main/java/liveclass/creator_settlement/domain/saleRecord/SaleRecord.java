package liveclass.creator_settlement.domain.saleRecord;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sale_records",
        indexes = {
                @Index(name = "idx_sale_record_course_id", columnList = "course_id"),
                @Index(name = "idx_sale_record_paid_at", columnList = "paid_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_salerecord_course_student", columnNames = {"course_id", "student_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleRecord {
    @Id
    public String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SaleRecordStatus status;

    @Column(name="course_id", nullable = false)
    public String courseId;

    @Column(name="student_id", nullable = false)
    public String studentId;

    @Column(nullable = false, updatable = false, precision = 6)
    public BigDecimal amount;

//    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    public LocalDateTime paidAt;


    public static SaleRecord of(String id, String courseId, String studentId, BigDecimal amount, LocalDateTime paidAt) {
        SaleRecord record = new SaleRecord();
        record.id = id;
        record.courseId = courseId;
        record.studentId = studentId;
        record.amount = amount;
        record.status = SaleRecordStatus.PAID;
        record.paidAt = paidAt;
        return record;
    }

    public void cancel() {
        this.status = SaleRecordStatus.CANCELLED;
    }
}
