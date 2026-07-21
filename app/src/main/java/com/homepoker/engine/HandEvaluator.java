package com.homepoker.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluates the best 5-card poker hand from any set of 5 to 7 cards.
 *
 * <p>The algorithm scores every 5-card combination (at most C(7,5)=21) with an
 * exact rules-based classifier and returns the strongest. This is
 * deliberately combinatorial rather than a lookup table: it is trivially
 * auditable, correct by construction for every edge case (wheel straights,
 * kickers, split pots), and more than fast enough for a card game.
 */
public final class HandEvaluator {

    private HandEvaluator() {}

    /** Best hand from 5–7 cards (e.g. 2 hole + up to 5 community). */
    public static HandValue best(List<Card> cards) {
        if (cards.size() < 5 || cards.size() > 7) {
            throw new IllegalArgumentException("Need 5–7 cards, got " + cards.size());
        }
        HandValue best = null;
        int n = cards.size();
        // Choose 5 of n via nested index selection.
        int[] idx = new int[5];
        best = bestRecursive(cards, idx, 0, 0, n, null);
        return best;
    }

    private static HandValue bestRecursive(List<Card> cards, int[] idx, int start, int depth, int n, HandValue best) {
        if (depth == 5) {
            List<Card> five = new ArrayList<>(5);
            for (int i : idx) five.add(cards.get(i));
            HandValue v = evaluate5(five);
            if (best == null || v.compareTo(best) > 0) return v;
            return best;
        }
        for (int i = start; i <= n - (5 - depth); i++) {
            idx[depth] = i;
            best = bestRecursive(cards, idx, i + 1, depth + 1, n, best);
        }
        return best;
    }

    /** Convenience: hole cards + board. */
    public static HandValue best(List<Card> hole, List<Card> board) {
        List<Card> all = new ArrayList<>(hole.size() + board.size());
        all.addAll(hole);
        all.addAll(board);
        return best(all);
    }

    /** Classify an exact 5-card hand. */
    public static HandValue evaluate5(List<Card> five) {
        if (five.size() != 5) throw new IllegalArgumentException("evaluate5 needs exactly 5 cards");

        // Sort descending by rank value.
        List<Card> sorted = new ArrayList<>(five);
        sorted.sort(Comparator.comparingInt((Card c) -> c.rank.value).reversed());

        boolean flush = isFlush(sorted);
        int straightHigh = straightHighCard(sorted); // 0 if not a straight

        // Rank multiplicities, highest count then highest rank first.
        // counts[rankValue] = occurrences
        int[] counts = new int[15]; // index by rank.value (2..14)
        for (Card c : sorted) counts[c.rank.value]++;

        // Build groups: array of [count, rankValue] sorted by count desc, then rankValue desc.
        List<int[]> groups = new ArrayList<>();
        for (int v = 14; v >= 2; v--) {
            if (counts[v] > 0) groups.add(new int[]{counts[v], v});
        }
        groups.sort((a, b) -> a[0] != b[0] ? b[0] - a[0] : b[1] - a[1]);

        int[] pattern = new int[groups.size()];
        for (int i = 0; i < groups.size(); i++) pattern[i] = groups.get(i)[0];

        if (straightHigh > 0 && flush) {
            return new HandValue(HandCategory.STRAIGHT_FLUSH, new int[]{straightHigh}, sorted);
        }
        if (pattern.length > 0 && pattern[0] == 4) {
            // Four of a kind + 1 kicker.
            return new HandValue(HandCategory.FOUR_OF_A_KIND,
                    new int[]{groups.get(0)[1], groups.get(1)[1]}, sorted);
        }
        if (pattern.length >= 2 && pattern[0] == 3 && pattern[1] == 2) {
            return new HandValue(HandCategory.FULL_HOUSE,
                    new int[]{groups.get(0)[1], groups.get(1)[1]}, sorted);
        }
        if (flush) {
            return new HandValue(HandCategory.FLUSH, topRanks(sorted, 5), sorted);
        }
        if (straightHigh > 0) {
            return new HandValue(HandCategory.STRAIGHT, new int[]{straightHigh}, sorted);
        }
        if (pattern.length > 0 && pattern[0] == 3) {
            // Trips + two kickers.
            int[] tb = new int[]{groups.get(0)[1], groups.get(1)[1], groups.get(2)[1]};
            return new HandValue(HandCategory.THREE_OF_A_KIND, tb, sorted);
        }
        if (pattern.length >= 2 && pattern[0] == 2 && pattern[1] == 2) {
            // Two pair: high pair, low pair, kicker.
            int[] tb = new int[]{groups.get(0)[1], groups.get(1)[1], groups.get(2)[1]};
            return new HandValue(HandCategory.TWO_PAIR, tb, sorted);
        }
        if (pattern.length > 0 && pattern[0] == 2) {
            // One pair + three kickers.
            int[] tb = new int[]{groups.get(0)[1], groups.get(1)[1], groups.get(2)[1], groups.get(3)[1]};
            return new HandValue(HandCategory.PAIR, tb, sorted);
        }
        return new HandValue(HandCategory.HIGH_CARD, topRanks(sorted, 5), sorted);
    }

    private static boolean isFlush(List<Card> five) {
        Suit s = five.get(0).suit;
        for (Card c : five) if (c.suit != s) return false;
        return true;
    }

    /**
     * Returns the high card rank of a 5-card straight, or 0 if not a straight.
     * Handles the wheel (A-2-3-4-5) as a 5-high straight.
     */
    private static int straightHighCard(List<Card> fiveDesc) {
        // Distinct ranks required for a straight.
        int[] r = new int[5];
        for (int i = 0; i < 5; i++) r[i] = fiveDesc.get(i).rank.value;
        // Normal straight: strictly decreasing by 1.
        boolean normal = true;
        for (int i = 1; i < 5; i++) {
            if (r[i] != r[i - 1] - 1) { normal = false; break; }
        }
        if (normal) return r[0];
        // Wheel: A,5,4,3,2 -> treat ace as low. Sorted desc that is 14,5,4,3,2.
        if (r[0] == 14 && r[1] == 5 && r[2] == 4 && r[3] == 3 && r[4] == 2) {
            return 5;
        }
        return 0;
    }

    private static int[] topRanks(List<Card> sortedDesc, int count) {
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = sortedDesc.get(i).rank.value;
        return out;
    }
}
