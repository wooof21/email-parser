package com.example.emailparser.config;

import com.example.emailparser.handler.MailHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationDsl;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mail.dsl.Mail;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

@EnableIntegration
@Configuration
@AllArgsConstructor
public class MailConnectionConfig {

    private final MailServerProps mailServerProps;

    private final MailHandler mailHandler;

    /**
    * By Default, JAVA MAIL API will filer on emails with UNREAD and UNFLAGGED status once an email is
    * processed, the flag (SEEN, FLAGGED) will be added Overwrite the default searchTermStrategy that
     * filter on emails that are UNREAD(SEEN flag) so that emails that were already been processed,
     * when it was marked as UNREAD manually it will be reprocessed again
    * **/
    @Bean
    public IntegrationFlow imapMailFlow() {
        return IntegrationFlow.from(Mail.imapInboundAdapter(getMailConnStr()))
                .showldMarkMessagesAsRead(true)
                .searchTermStrategy(this::searchTerm)
                .shouldDeleteMessages(mailServerProps.getDeleteMessages())
                .simpleContent(true)
                .autoCloseFolder(false)
                .maxFetchSize(mailServerProps.getMaxFetchSize())
                .javaMailProperties(p -> p.put("mail.debug", mailServerProps.getDebug())
                        .put("mail.imap.connectionpoolsize", mailServerProps.getConnectionPoolSize())
                ), c -> c.poller(Pollers.fixedRate(mailServerProps.getPollRateInMilSecs())
                .maxMessagesPerPoll(mailServerProps.getMaxMsgPerPoll())))
                .handle(mailHandler, "handle")
                .get();
    }

    private String getMailConnStr() {
        return mailServerProps.getProtocol() + "://" +
                mailServerProps.getUn() + ":" +
                mailServerProps.getPw() + "@" +
                mailServerProps.getServer() + ":" +
                mailServerProps.getPort() + "/" +
                mailServerProps.getFolder();
    }

    private SearchTerm searchTerm(Flags supportedFlags, Folder folder) {
        SearchTerm[] searchTerms = {
                new FlagTerm(new Flags(Flags.Flag.SEEN), false)
        };
        return new AndTerm(searchTerms);
    }
}
