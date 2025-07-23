package com.example.emailparser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mail-listener")
public class MailServerProps {

    private String protocol;
    private String server;
    private String port;
    private String un;
    private String pw;
    private String folder;
    private Boolean debug;
    private Boolean deleteMessages;
    private String connectionPoolSize;
    private Boolean autoReconnect;
    private Long pollRateInMilSecs;
    private Integer maxMsgPerPoll;
    private Integer maxFetchSize;


}
