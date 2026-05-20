package com.jobskillsmatcher.context.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CacheConfig {

    public static final String READINESS_SCORE = "readinessScore";
    public static final String JOB_FEED = "jobFeed";
    public static final String ESCO_SKILLS = "escoSkills";

    @Bean
    @ConfigurationProperties(prefix = "jobskillsmatcher.cache")
    CacheTtls cacheTtls() {
        return new CacheTtls();
    }

    @Bean
    CacheManager cacheManager(CacheTtls ttls) {
        List<CaffeineCache> caches = new ArrayList<>();
        caches.add(new CaffeineCache(READINESS_SCORE,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttls.getReadinessScoreTtl())
                        .maximumSize(20_000)
                        .build()));
        caches.add(new CaffeineCache(JOB_FEED,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttls.getJobFeedTtl())
                        .maximumSize(2_000)
                        .build()));
        caches.add(new CaffeineCache(ESCO_SKILLS,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttls.getEscoSkillsTtl())
                        .maximumSize(2_000)
                        .build()));
        SimpleCacheManager mgr = new SimpleCacheManager();
        mgr.setCaches(caches);
        return mgr;
    }

    public static class CacheTtls {
        private Duration readinessScoreTtl = Duration.ofMinutes(2);
        private Duration jobFeedTtl = Duration.ofMinutes(5);
        private Duration escoSkillsTtl = Duration.ofHours(1);

        public Duration getReadinessScoreTtl() { return readinessScoreTtl; }
        public void setReadinessScoreTtl(Duration v) { this.readinessScoreTtl = v; }
        public Duration getJobFeedTtl() { return jobFeedTtl; }
        public void setJobFeedTtl(Duration v) { this.jobFeedTtl = v; }
        public Duration getEscoSkillsTtl() { return escoSkillsTtl; }
        public void setEscoSkillsTtl(Duration v) { this.escoSkillsTtl = v; }
    }
}
