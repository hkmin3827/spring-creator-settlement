package liveclass.creator_settlement.domain.creator;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name="creators")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Creator {
    @Id
    public String id;

    @Column(length = 30)
    public String name;

    public static Creator of(String id, String name) {
        Creator creator = new Creator();
        creator.id = id;
        creator.name = name;
        return creator;
    }
}
