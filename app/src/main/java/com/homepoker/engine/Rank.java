package com.homepoker.engine;

/**
 * Card ranks. {@link #value} is used for straight/high-card comparisons.
 * Ace is high by default (14) but is treated as 1 for the "wheel" (A-2-3-4-5)
 * straight inside {@link HandEvaluator}.
 */
public enum Rank {
    TWO(2, '2'), THREE(3, '3'), FOUR(4, '4'), FIVE(5, '5'), SIX(6, '6'),
    SEVEN(7, '7'), EIGHT(8, '8'), NINE(9, '9'), TEN(10, 'T'),
    JACK(11, 'J'), QUEEN(12, 'Q'), KING(13, 'K'), ACE(14, 'A');

    public final int value;
    public final char code;

    Rank(int value, char code) {
        this.value = value;
        this.code = code;
    }

    public static Rank fromCode(char c) {
        char u = Character.toUpperCase(c);
        for (Rank r : values()) {
            if (r.code == u) return r;
        }
        throw new IllegalArgumentException("Unknown rank code: " + c);
    }
}
