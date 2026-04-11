package liveclass.creator_settlement.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache creatorInfoCache =
                new CaffeineCache(
                        "creator-name",
                        Caffeine.newBuilder()
                                .maximumSize(1000000)
                                .build()
                );


        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                creatorInfoCache
        ));

        return manager;
    }
}