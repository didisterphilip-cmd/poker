package com.homepoker.ai;

import com.homepoker.engine.Card;
import com.homepoker.engine.HandEvaluator;
import com.homepoker.engine.Rank;
import com.homepoker.engine.Suit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Estimates the win probability (equity) of a hole-card holding against a number
 * of unknown opponents via Monte-Carlo roll-outs. Pure JVM code — no Android
 * dependencies — so it is unit-testable and runs identically on server or device.
 */
public final class HandStrength {

    private HandStrength() {}

    /**
     * @param hole      the player's two hole cards
     * @param board     0, 3, 4 or 5 community cards known so far
     * @param opponents number of live opponents
     * @param sims      roll-out count (200–400 is a good speed/accuracy trade-off)
     * @return estimated equity in [0,1] (win share, counting ties fractionally)
     */
    public static double equity(List<Card> hole, List<Card> board, int opponents, int sims, Random rng) {
        if (opponents <= 0) return 1.0;
        List<Card> deck = fullDeckExcluding(concat(hole, board));
        double score = 0;
        for (int s = 0; s < sims; s++) {
            shufflePrefix(deck, 2 * opponents + (5 - board.size()), rng);
            int di = 0;
            // Deal opponent hands.
            List<List<Card>> oppHoles = new ArrayList<>();
            for (int o = 0; o < opponents; o++) {
                List<Card> oh = new ArrayList<>(2);
                oh.add(deck.get(di++));
                oh.add(deck.get(di++));
                oppHoles.add(oh);
            }
            // Complete the board.
            List<Card> fullBoard = new ArrayList<>(board);
            while (fullBoard.size() < 5) fullBoard.add(deck.get(di++));

            var mine = HandEvaluator.best(new ArrayList<>(hole), fullBoard);
            boolean tie = false;
            boolean lose = false;
            int tieCount = 1;
            for (List<Card> oh : oppHoles) {
                var theirs = HandEvaluator.best(oh, fullBoard);
                int cmp = mine.compareTo(theirs);
                if (cmp < 0) { lose = true; break; }
                if (cmp == 0) { tie = true; tieCount++; }
            }
            if (lose) continue;
            score += tie ? 1.0 / tieCount : 1.0;
        }
        return score / sims;
    }

    private static List<Card> concat(List<Card> a, List<Card> b) {
        List<Card> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a); out.addAll(b);
        return out;
    }

    private static List<Card> fullDeckExcluding(List<Card> used) {
        Set<Card> u = new HashSet<>(used);
        List<Card> deck = new ArrayList<>(52);
        for (Suit s : Suit.values())
            for (Rank r : Rank.values()) {
                Card c = new Card(r, s);
                if (!u.contains(c)) deck.add(c);
            }
        return deck;
    }

    /** Partial Fisher-Yates: only the first {@code k} positions need to be random. */
    private static void shufflePrefix(List<Card> deck, int k, Random rng) {
        int n = deck.size();
        for (int i = 0; i < k && i < n - 1; i++) {
            int j = i + rng.nextInt(n - i);
            Card tmp = deck.get(i); deck.set(i, deck.get(j)); deck.set(j, tmp);
        }
    }
}
