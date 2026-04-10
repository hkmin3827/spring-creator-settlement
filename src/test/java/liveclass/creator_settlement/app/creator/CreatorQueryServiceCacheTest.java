package liveclass.creator_settlement.app.creator;

import liveclass.creator_settlement.domain.creator.Creator;
import liveclass.creator_settlement.domain.creator.CreatorRepository;
import liveclass.creator_settlement.global.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {CreatorQueryService.class, CacheConfig.class})
class CreatorQueryServiceCacheTest {

    @Autowired
    private CreatorQueryService creatorQueryService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CreatorRepository creatorRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("creator-name").clear();
        Creator creator = Creator.of("creator-1", "홍길동");
        given(creatorRepository.findById("creator-1")).willReturn(Optional.of(creator));
    }

    @Test
    void 같은_크리에이터_이름을_두_번_조회하면_DB는_한_번만_호출된다() {
        String first = creatorQueryService.getCreatorName("creator-1");
        String second = creatorQueryService.getCreatorName("creator-1");

        assertThat(first).isEqualTo("홍길동");
        assertThat(second).isEqualTo("홍길동");
        verify(creatorRepository, times(1)).findById("creator-1");
    }

    @Test
    void 캐시_evict_후_재조회하면_DB를_다시_호출한다() {
        creatorQueryService.getCreatorName("creator-1");
        creatorQueryService.evictCreatorName("creator-1");
        creatorQueryService.getCreatorName("creator-1");

        verify(creatorRepository, times(2)).findById("creator-1");
    }
}
