package com.example.emailparser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "email-alert-error-routing")
public class EmailAlertErrorProps {

    private List<String> invalidEmailAddressErrorCodes;

    private Integer rightShiftOffset;

    private Map<String, List<String>> keywords;

    private Map<String, ErrorCodeObject> enhancedCodes;

    private Map<String, ErrorCodeObject> traditionalCodes;

    @Data
    public class ErrorCodeObject {
        private String type;

        private String description;
    }
}
