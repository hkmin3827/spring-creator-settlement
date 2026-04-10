package liveclass.creator_settlement.app.creator;

import liveclass.creator_settlement.domain.creator.CreatorRepository;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorQueryService {

    private final CreatorRepository creatorRepository;

    @Cacheable(value = "creator-name", key = "#creatorId")
    public String getCreatorName(String creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND))
                .name;
    }

    public Map<String, String> getAllCreatorNames() {
        return creatorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(c -> c.id, c -> c.name));
    }

    @CacheEvict(value = "creator-name", key = "#creatorId")
    public void evictCreatorName(String creatorId) {}
}
