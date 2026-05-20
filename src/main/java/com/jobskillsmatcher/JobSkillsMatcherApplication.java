package com.jobskillsmatcher;

import com.jobskillsmatcher.context.web.SecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableRetry
@Import(SecurityConfiguration.class)
public class JobSkillsMatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSkillsMatcherApplication.class, args);
    }

}
