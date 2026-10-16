package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** Formal register for official correspondence (faculty, clients, HR). */
@Component
public class FormalReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.FORMAL;
    }

    @Override
    protected String greeting(String name) {
        return "Dear " + name + ",";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("I am writing in response to your correspondence concerning \"")
                .append(originalEmail.getSubject()).append("\". ");
        body.append(actionItemsSentence(analysis, "The following items will be duly attended to: "));
        body.append(deadlineSentence(analysis, "This matter will be resolved no later than "));
        body.append("Should you require any additional information, please do not hesitate to contact me.");
        return body.toString();
    }

    @Override
    protected String closing() {
        return "Yours sincerely,\nSmartReply AI (Demo User)";
    }
}
