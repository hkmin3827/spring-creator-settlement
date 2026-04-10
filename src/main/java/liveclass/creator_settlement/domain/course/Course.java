package liveclass.creator_settlement.domain.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id
    public String id;

    @Column(name = "creator_id", nullable = false)
    public Long creatorId;

    @Column(length = 255)
    public String title;


    @Column(precision = 19, scale =2)
    public BigDecimal price;
}
