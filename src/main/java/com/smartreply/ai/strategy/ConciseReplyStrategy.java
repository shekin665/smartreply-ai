package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

/** Short, to-the-point tone - minimum words, maximum clarity. */
@Component
public class ConciseReplyStrategy extends AbstractReplyStrategy {

    @Override
    public Tone getTone() {
        return Tone.CONCISE;
    }

    @Override
    protected String greeting(String name) {
        return "Hi " + name + ",";
    }

    @Override
    protected String bodyText(Email originalEmail, KeywordAnalysis analysis) {
        StringBuilder body = new StringBuilder();
        body.append("Got it - on it. ");
        body.append(actionItemsSentence(analysis, "Next steps: "));
        body.append(deadlineSentence(analysis, "Done by "));
        if (analysis.getActionItems().isEmpty() && analysis.getDates().isEmpty()) {
            body.append("Will follow up shortly.");
        }
        return body.toString();
    }

    @Override
    protected String closing() {
        return "Thanks,\nSmartReply AI (Demo User)";
    }
}
