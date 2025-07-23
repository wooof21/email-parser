package com.example.emailparser.handler;

import com.example.emailparser.config.EmailAlertErrorProps;
import com.example.emailparser.model.EmailBounceBackDetails;
import org.springframework.beans.factory.annotation.Autowired;

public class TempEmailFailureBBHandler {

    @Autowired
    private AlertService alertService;

    @Autowired
    private EmailAlertErrorProps emailAlertErrorProps;

    @Autowired
    private AlertResendService alertResendService;

    public void processBBErrors(EmailBounceBackDetails error) {

    }
}
