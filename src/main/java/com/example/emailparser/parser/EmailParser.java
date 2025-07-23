package com.example.emailparser.parser;

import com.example.emailparser.config.EmailAlertErrorProps;
import com.example.emailparser.model.EmailBounceBackDetails;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.example.emailparser.config.Constants.*;

@Slf4j
@Component
@AllArgsConstructor
public class EmailParser {

    private final EmailAlertErrorProps props;

    public EmailBounceBackDetails parseEmail(String message) {
        return EmailBounceBackDetails.builder().build();
    }

    public Long retrieveBounceBackTime(String content) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[dd MMM yyyy HH:mm:ss]" + "[d MMM yyyy HH:mm:ss]");

            List<String> firstStepSearchKeywords = props.getKeywords().get(KEYWORD_TIME);

            Emit firstStepEmit = contentWithoutOverlapping(content, firstStepSearchKeywords);
            if(firstStepEmit == null) return null;

            int endPosition = firstStepEmit.getEnd();
            String subcontent = content.substring(endPosition + 8, endPosition + 28).trim();
            LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(subcontent));

            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(""));
            return zonedDateTime.minusHours(5).toEpochSecond();
        } catch (Exception e) {
            return Instant.now().getEpochSecond();
        }
    }

    /**
     * 2 step search for failureCodes, an one step search for 3 digits code will not return an accurate
     * result as other failure code may exist somewhere in email content(i.e. recipient's address)
     * and it comes before where the actual failure code, which result the searching return the first
     * matched failure code.
     *
     * Step 1: search for the sub content where the faulireCode may exist using an unique keyword
     *          (same bounced template for all email provider)
     * Step 2: if no content found, return empty string
     *          else get the ending position of the found keyword, right shift 20 chars and get the
     *          substring, then use this substring as the content to search for 3 digits code.
     * @param content
     * @return
     */
    public String retrieveFailureCode(String content) {
        List<String> failureCodes = props.getKeywords().get("failureCodes");
        Emit emit = contentWithoutOverlapping(content, failureCodes);
        if(emit == null) return null;
        int endPos = emit.getEnd();
        String subContent = content.substring(endPos + SUB_STRING_SHIFT_OFFSET, endPos + props.getRightShiftOffset());

        List<String> traditionalCodes = new ArrayList<>(props.getTraditionalCodes().keySet());

        Emit result = contentWithoutOverlapping(subContent, traditionalCodes);

        if(result == null) {
            log.error("The error code was not defined in application.yml, " +
                    "the untracked error code may show here: {}", subContent);
            return null;
        }
        return result.getKeyword();
    }

    /**
     * Same step as retrieveFailureCode, use enhancedFailureCode keywords
     * @param content
     * @return
     */
    public String retrieveEnhancedFailureCode(String content) {
        List<String> firstStepkeywords = props.getKeywords().get(KEYWORD_FAILURE_CODE);

        Emit firstEmit = contentWithoutOverlapping(content, firstStepkeywords);
        if(firstEmit == null) return null;

        int endPos = firstEmit.getEnd();
        String subcontent = content.substring(endPos + SUB_STRING_SHIFT_OFFSET, endPos + props.getRightShiftOffset());

        List<String> traditionalCode = new ArrayList<>(props.getEnhancedCodes().keySet());

        Emit result = contentWithoutOverlapping(subcontent, traditionalCode);

        return result != null ? result.getKeyword() : null;
    }

    /**
     * with preceding @ symbol, search the keywords for all matching,
     * Recipient's address (email provider host) comes first in email body, stopOnHit will
     * return the first match from the content
     * @param content
     * @return
     */
    public String retrieveEmailProvider(String content) {
        List<String> emailHostList = props.getKeywords().get(KEYWORD_EMAIL);
        Emit providerEmit = contentStopOnHit(content, emailHostList);
        return providerEmit != null ? providerEmit.getKeyword() : null;
    }

    /**
     * Search for description using 2 phrases as keywords (description exist in between).
     * get the substring using the ending position of top phrases, and the starting
     * position of bottom phrase.
     * @param content
     * @return
     */
    public String retrieveErrorDescription(String content) {
        List<String> descriptionKeywords = props.getKeywords().get(KEYWORD_DESCRIPTION);
        Collection<Emit> foundEmits = contentInBetween(content, descriptionKeywords);
        Emit[] foundEmitsArray = foundEmits.toArray(new Emit[foundEmits.size()]);
        int topPos, bottomPos;

        if(foundEmitsArray.length == 0) return "Failed to extract error description";
        if(foundEmitsArray[0].getEnd() < foundEmitsArray[1].getStart()) {
            topPos = foundEmitsArray[0].getEnd();
            bottomPos = foundEmitsArray[1].getStart();
        } else {
            topPos = foundEmitsArray[1].getEnd();
            bottomPos = foundEmitsArray[0].getStart();
        }

        return content.substring(topPos + SUB_STRING_SHIFT_OFFSET, bottomPos)
                .replace(CHAR_RETURN_STRING, CHAR_REPLACEMENT)
                .replace(CHAR_NEW_LINE_STRING, CHAR_REPLACEMENT)
                .replace(CHAR_TAP_STRING, CHAR_REPLACEMENT);
    }

    /**
     * Search for description keywords and search for onlyWholeWords,
     * return the Collection<Emit> that contains the top and bottom keywords with starting
     * abd ending position.
     * @param content
     * @param keywords
     * @return
     */
    private Collection<Emit> contentInBetween(String content, List<String> keywords) {
        Trie trie = Trie.builder()
                .onlyWholeWords().addKeywords(keywords).build();
        return trie.parseText(content);
    }

    /**
     * Search for the keywords and ignore overlaps, used with 2 step search to search for failureCode
     * or enhancedFailureCode, which are guaranteed to have the matched keyword to be the actual
     * failure code.
     *
     * ignore overlap: In situation where overlapping instances are not desired, retain:
     *          1. larger marches prevail over shorter matches, and
     *          2. left-most prevail over right-most, and
     *          3. Only one result is returned
     * eg. content: 5.1.10
     *      keywords: [5.1.1, 5.1.10]
     *      return 5.1.10
     * @param content
     * @param keywords
     * @return
     */
    private Emit contentWithoutOverlapping(String content, List<String> keywords) {
        Trie trie = Trie.builder()
                .onlyWholeWords().ignoreOverlaps().addKeywords(keywords).build();
        Iterator<Emit> emits = trie.parseText(content).iterator();
        return emits.hasNext() ? emits.next() : null;
    }

    /**
     * Search for keywords and stopOnHit, used to search for keywords with preceding special char,
     * instead of only searching for words, but search for the whole keyword.
     * @param content
     * @param keywords
     * @return
     */

    private Emit contentStopOnHit(String content, List<String> keywords) {
        Trie trie = Trie.builder()
                .stopOnHit().addKeywords(keywords).build();
        Iterator<Emit> emits = trie.parseText(content).iterator();
        return emits.hasNext() ? emits.next() : null;
    }
}
