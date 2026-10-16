package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** Balanced, business-appropriate tone - the default for most work email. */
@Component
public class ProfessionalReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.PROFESSIONAL;
    }

    @Override
    protected String greeting(String name) {
        return "Hi " + name + ",";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your email regarding \"").append(originalEmail.getSubject())
                .append("\". I've reviewed the details. ");
        body.append(actionItemsSentence(analysis, "I will take care of the following: "));
        body.append(deadlineSentence(analysis, "I'll make sure this is addressed by "));
        body.append("Please let me know if you need anything further in the meantime.");
        return body.toString();
    }

    @Override
    protected String closing() {
        return "Best regards,\nSmartReply AI (Demo User)";
    }
}
