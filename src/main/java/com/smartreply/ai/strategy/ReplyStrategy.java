package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Tone;

/**
 * OOP concept - Abstraction via an interface: every concrete tone strategy
 * must be able to say which Tone it handles and how to turn one email (plus
 * its keyword analysis) into reply text, but the "how" is left entirely to
 * the implementing class.
 * <p>
 * OOP concept - Polymorphism: ReplyService holds a collection of
 * ReplyStrategy references and calls generate() on whichever one matches
 * the requested tone - the JVM dispatches to the correct overridden method
 * at runtime without ReplyService knowing the concrete class.
 */
public interface ReplyStrategy {

    Tone getTone();

    String generate(Email originalEmail, KeywordAnalysis analysis);
}
