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

    @Column(name = "creator_name", nullable = false)
    public String creatorName;

    @Column(length = 255)
    public String title;

    @Column(precision = 10, scale =2)
    public BigDecimal price;

    public static Course of(String id, String creatorId, String title) {
        Course course = new Course();
        course.id = id;
        course.creatorId = creatorId;
        course.title = title;
        return course;
    }
}
