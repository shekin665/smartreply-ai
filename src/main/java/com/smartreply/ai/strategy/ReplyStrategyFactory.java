package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * OOP concept - Dependency Injection + Polymorphism: Spring collects every
 * {@code @Component} that implements ReplyStrategy (the five tone classes)
 * into one {@code List<ReplyStrategy>} and injects it here. The factory
 * never names a concrete class - it indexes the beans by {@code getTone()}
 * and hands back whichever one the caller asked for, so adding a sixth
 * tone later only means adding a new ReplyStrategy bean.
 */
@Component
public class ReplyStrategyFactory {

    private final Map<Tone, ReplyStrategy> strategiesByTone = new EnumMap<>(Tone.class);

    public ReplyStrategyFactory(List<ReplyStrategy> strategies) {
        for (ReplyStrategy strategy : strategies) {
            strategiesByTone.put(strategy.getTone(), strategy);
        }
    }

    public ReplyStrategy getStrategy(Tone tone) {
        ReplyStrategy strategy = strategiesByTone.get(tone);
        if (strategy == null) {
            throw new IllegalArgumentException("No reply strategy registered for tone: " + tone);
        }
        return strategy;
    }
}
