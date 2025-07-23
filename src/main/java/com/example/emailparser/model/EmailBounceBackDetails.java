package com.example.emailparser.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailBounceBackDetails {

    private String failureMessage;
    private String failureCode;
    private String enhancedFailureCode;
    private String bounceBackClassification;
    private String emailProvider;
    private Long timestamp;
}
