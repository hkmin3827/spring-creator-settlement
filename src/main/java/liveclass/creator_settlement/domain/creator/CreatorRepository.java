package liveclass.creator_settlement.domain.creator;

import liveclass.creator_settlement.app.creator.dto.CreatorNameDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CreatorRepository extends JpaRepository<Creator, String> {
    @Query("SELECT new liveclass.creator_settlement.app.dto.CreatorNameDto(c.id, c.name) FROM Creator c")
    List<CreatorNameDto> findIdAndName();
}
