package liveclass.creator_settlement.domain.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "courses",
        indexes = @Index(name = "idx_course_creator_id", columnList = "creator_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id
    public String id;

    @Column(name = "creator_id", nullable = false)
    public String creatorId;

    @Column(length = 255)
    public String title;

    @Column(precision = 6, scale =0, nullable = false)    // 최대 99만원
    public BigDecimal price = BigDecimal.ZERO;

    public static Course of(String id, String creatorId, String title, BigDecimal price) {
        Course course = new Course();
        course.id = id;
        course.price = price;
        course.creatorId = creatorId;
        course.title = title;
        return course;
    }
}
