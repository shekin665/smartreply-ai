package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** A fuller, more thorough reply. Used by the "More detailed" option. */
@Component
public class DetailedReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.DETAILED;
    }

    @Override
    protected String greeting(String name) {
        return "Dear " + name + ",";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your email regarding \"").append(originalEmail.getSubject()).append("\". ");
        body.append("I have read your message carefully and will keep you updated on progress. ");
        body.append(actionItemsSentence(analysis, "I will take care of the following: "));
        body.append(deadlineSentence(analysis, "I will make sure this is completed by "));
        body.append("Please feel free to contact me if you need any further details.");
        return body.toString();
    }

    @Override
    protected String closing() {
        return "Kind regards,\nSmartReply AI (Demo User)";
    }
}
