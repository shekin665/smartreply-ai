package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** Tone for when a reply needs to acknowledge a delay, mistake or complaint. */
@Component
public class ApologeticReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.APOLOGETIC;
    }

    @Override
    protected String greeting(String name) {
        return "Hi " + name + ",";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("I'm sorry for the delay in responding to \"").append(originalEmail.getSubject())
                .append("\" - that's on me. ");
        body.append(actionItemsSentence(analysis, "To put things right, I will: "));
        body.append(deadlineSentence(analysis, "I'll make sure this is sorted by "));
        body.append("Thank you for your patience, and please let me know if this has caused any inconvenience.");
        return body.toString();
    }

    @Override
    protected String closing() {
        return "With apologies,\nSmartReply AI (Demo User)";
    }
}
