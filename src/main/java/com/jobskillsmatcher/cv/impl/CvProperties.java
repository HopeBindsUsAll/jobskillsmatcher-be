package com.jobskillsmatcher.cv.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jobskillsmatcher.cv")
public class CvProperties {

    private long maxBytes = 10L * 1024 * 1024;
    private List<String> acceptedContentTypes = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
}
