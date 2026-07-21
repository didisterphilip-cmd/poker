package com.homepoker.engine;

import java.util.Arrays;
import java.util.List;

/**
 * The strength of a made 5-card hand: a {@link HandCategory} plus an ordered
 * list of tiebreak ranks (kickers). Two hands are compared first by category,
 * then element-by-element on the tiebreak array — exactly the way poker
 * resolves kickers and split pots.
 *
 * <p>Example: two players both holding a pair of Kings are ranked by their
 * remaining three kickers in descending order; equal arrays mean a tied hand
 * (a split pot).
 */
public final class HandValue implements Comparable<HandValue> {
    public final HandCategory category;
    /** Tiebreak ranks, most significant first. Never null. */
    public final int[] tiebreak;
    /** The five cards that make up this hand (for display / hand history). */
    public final List<Card> cards;

    public HandValue(HandCategory category, int[] tiebreak, List<Card> cards) {
        this.category = category;
        this.tiebreak = tiebreak;
        this.cards = cards;
    }

    @Override
    public int compareTo(HandValue o) {
        int c = Integer.compare(category.ordinal(), o.category.ordinal());
        if (c != 0) return c;
        int n = Math.min(tiebreak.length, o.tiebreak.length);
        for (int i = 0; i < n; i++) {
            c = Integer.compare(tiebreak[i], o.tiebreak[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    /** True if this hand is exactly as strong as {@code o} (a tie / split). */
    public boolean ties(HandValue o) {
        return compareTo(o) == 0;
    }

    /** Whether this is the ace-high straight flush. */
    public boolean isRoyalFlush() {
        return category == HandCategory.STRAIGHT_FLUSH && tiebreak.length > 0 && tiebreak[0] == Rank.ACE.value;
    }

    public String describe() {
        if (isRoyalFlush()) return "Royal Flush";
        return category.label;
    }

    @Override
    public String toString() {
        return describe() + " " + Arrays.toString(tiebreak);
    }
}
