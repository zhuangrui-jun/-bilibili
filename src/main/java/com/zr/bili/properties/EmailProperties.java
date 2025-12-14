package com.zr.bili.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email.code")
@Data
public class EmailProperties {
    private Integer expire;
    private Integer limit;
    private String subject;
}
