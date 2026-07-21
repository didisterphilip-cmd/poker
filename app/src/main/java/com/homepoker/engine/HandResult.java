package com.homepoker.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The outcome of a completed hand: pot awards and showdown information. */
public final class HandResult {
    /** Chips won by each player this hand (net of what they put in is not applied here). */
    public final Map<Player, Integer> winnings = new LinkedHashMap<>();
    /** Ordered pots that were awarded, with the winners of each. */
    public final List<Award> awards = new ArrayList<>();
    /** Board at showdown. */
    public final List<Card> board = new ArrayList<>();
    /** True if the hand ended because everyone else folded (no showdown). */
    public boolean wonByFold;

    public static final class Award {
        public final int potAmount;
        public final List<Player> winners = new ArrayList<>();
        public final Map<Player, Integer> payout = new LinkedHashMap<>();
        public HandValue winningHand; // null when won by fold

        public Award(int potAmount) {
            this.potAmount = potAmount;
        }
    }

    void addWinnings(Player p, int amount) {
        winnings.merge(p, amount, Integer::sum);
    }
}
