package com.homepoker.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds main and side pots from the amounts each player committed this hand.
 *
 * <p>The classic algorithm: repeatedly find the smallest non-zero commitment
 * among players still contributing, carve off a pot layer of that size from
 * every contributor, and mark the players who are eligible to win it (everyone
 * who is not folded and contributed to that layer). Folded players' chips stay
 * in the pots but they cannot win.
 */
public final class SidePots {

    private SidePots() {}

    /**
     * @param players all players who put chips in this hand (folded or not)
     * @return ordered list of pots, main pot first
     */
    public static List<Pot> build(List<Player> players) {
        // Work on a copy of remaining commitments.
        List<Player> contributors = new ArrayList<>();
        int[] remaining = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.totalCommitted > 0) {
                contributors.add(p);
                remaining[contributors.size() - 1] = p.totalCommitted;
            }
        }

        List<Pot> pots = new ArrayList<>();
        // Compact remaining[] to contributor size.
        int[] rem = new int[contributors.size()];
        for (int i = 0; i < contributors.size(); i++) rem[i] = contributors.get(i).totalCommitted;

        while (true) {
            // Smallest positive remaining among players who are still eligible to win
            // (not folded) OR still owe into a layer. A layer's size is the smallest
            // positive remaining across ALL contributors (folded chips form layers too).
            int layer = Integer.MAX_VALUE;
            for (int i = 0; i < rem.length; i++) {
                if (rem[i] > 0) layer = Math.min(layer, rem[i]);
            }
            if (layer == Integer.MAX_VALUE) break; // nothing left

            Pot pot = new Pot(0);
            for (int i = 0; i < rem.length; i++) {
                if (rem[i] > 0) {
                    pot.amount += layer;
                    rem[i] -= layer;
                    Player p = contributors.get(i);
                    if (!p.folded) pot.eligible.add(p);
                }
            }
            // Merge into previous pot if the eligibility set is identical (keeps pot list tidy).
            if (!pots.isEmpty() && sameEligibility(pots.get(pots.size() - 1), pot)) {
                pots.get(pots.size() - 1).amount += pot.amount;
            } else {
                pots.add(pot);
            }
        }

        // A pot whose eligible players all folded (its only contributors folded away)
        // has no rightful winner. Those chips become dead money merged into the nearest
        // lower pot that still has live eligible players. The lowest (main) pot always
        // has eligible players whenever the hand reaches showdown, so this terminates.
        List<Pot> cleaned = new ArrayList<>();
        for (Pot pot : pots) {
            if (pot.eligible.isEmpty() && !cleaned.isEmpty()) {
                cleaned.get(cleaned.size() - 1).amount += pot.amount;
            } else {
                cleaned.add(pot);
            }
        }
        return cleaned;
    }

    private static boolean sameEligibility(Pot a, Pot b) {
        if (a.eligible.size() != b.eligible.size()) return false;
        return a.eligible.containsAll(b.eligible);
    }
}
