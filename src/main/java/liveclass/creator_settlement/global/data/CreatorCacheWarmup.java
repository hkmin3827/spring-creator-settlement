package liveclass.creator_settlement.global.data;

import liveclass.creator_settlement.domain.creator.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class CreatorCacheWarmup implements ApplicationRunner {

    private final CreatorRepository creatorRepository;
    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        Cache cache = cacheManager.getCache("creator-name");
        if (cache == null) return;
        creatorRepository.findAll()
                .forEach(c -> cache.put(c.id, c.name));
    }
}
