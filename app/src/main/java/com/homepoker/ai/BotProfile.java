package com.homepoker.ai;

/**
 * A bot's personality. These knobs shape how the {@link PokerBot} turns an
 * equity estimate into an action, so different seats feel like different people
 * rather than one repeated opponent.
 */
public final class BotProfile {
    public final String styleName;
    /** Minimum equity to continue when facing a bet (lower = looser/calls more). */
    public final double callThreshold;
    /** Equity above which the bot wants to raise for value. */
    public final double raiseThreshold;
    /** How large value raises are, as a fraction of the pot (0.5–1.0 typical). */
    public final double raiseSizing;
    /** Probability of firing a bluff with a weak hand when checked to / heads-up. */
    public final double bluffFrequency;
    /** Randomness added to thresholds each decision, for unpredictability. */
    public final double noise;

    public BotProfile(String styleName, double callThreshold, double raiseThreshold,
                      double raiseSizing, double bluffFrequency, double noise) {
        this.styleName = styleName;
        this.callThreshold = callThreshold;
        this.raiseThreshold = raiseThreshold;
        this.raiseSizing = raiseSizing;
        this.bluffFrequency = bluffFrequency;
        this.noise = noise;
    }

    // ---- Difficulty presets -------------------------------------------------

    /** Tight-aggressive: folds junk, punishes with big value bets. Hard to beat. */
    public static BotProfile tightAggressive() {
        return new BotProfile("Tight-Aggressive", 0.45, 0.62, 0.85, 0.10, 0.04);
    }

    /** Loose-passive "calling station": sees lots of flops, rarely raises. */
    public static BotProfile loosePassive() {
        return new BotProfile("Loose-Passive", 0.28, 0.80, 0.5, 0.03, 0.06);
    }

    /** Maniac bluffer: fires often, unpredictable, applies pressure. */
    public static BotProfile bluffer() {
        return new BotProfile("Bluffer", 0.33, 0.55, 0.9, 0.32, 0.08);
    }

    /** Balanced solid player. */
    public static BotProfile balanced() {
        return new BotProfile("Balanced", 0.38, 0.66, 0.7, 0.14, 0.05);
    }

    // ---- Difficulty tiers ---------------------------------------------------

    public static BotProfile[] easy() {
        return new BotProfile[]{ loosePassive(), loosePassive(),
                new BotProfile("Loose-Passive", 0.25, 0.85, 0.45, 0.02, 0.10) };
    }

    public static BotProfile[] medium() {
        return new BotProfile[]{ balanced(), loosePassive(), bluffer() };
    }

    public static BotProfile[] hard() {
        return new BotProfile[]{ tightAggressive(), balanced(), bluffer(), tightAggressive() };
    }
}
