package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** Warm, casual tone for colleagues/friends. */
@Component
public class FriendlyReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.FRIENDLY;
    }

    @Override
    protected String greeting(String name) {
        return "Hey " + name + "!";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("Thanks for reaching out about \"").append(originalEmail.getSubject())
                .append("\" - good to hear from you! ");
        body.append(actionItemsSentence(analysis, "I'll go ahead and sort out: "));
        body.append(deadlineSentence(analysis, "No worries, I've got it covered before "));
        body.append("Let me know if anything else comes up!");
        return body.toString();
    }

    @Override
    protected String closing() {
        return "Cheers,\nSmartReply AI (Demo User)";
    }
}
