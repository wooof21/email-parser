package com.example.emailparser.handler;

import com.example.emailparser.model.EmailBounceBackDetails;
import com.example.emailparser.parser.EmailParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.mail.transformer.MailToStringTransformer;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Slf4j
@Service
public class MailHandler extends MailToStringTransformer implements GenericHandler<MimeMessage> {

    private final EmailParser parser;

    private final TempEmailFailureBBHandler bbHandler;

    private final AlertService alertService;

    @Async("bounceBackProcessorPool")
    public void handle(MimeMessage payload) {
        EmailBounceBackDetails details = null;
        String alertId = "";

        try {
            String mailLoad = doTransform(payload).getPayload();
            details = parser.parseEmail(mailLoad);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
