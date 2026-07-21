package com.homepoker.engine;

import java.util.ArrayList;
import java.util.List;

/** Mutable per-hand state for one seat at the table. */
public final class Player {
    public final String id;
    public String name;
    public boolean isBot;

    /** Chips in front of the player (their stack), excluding chips already in the pot. */
    public int chips;

    // ---- Per-hand transient state (reset by HandEngine at the start of each hand) ----
    public final List<Card> hole = new ArrayList<>(2);
    /** Chips committed on the current street. */
    public int streetBet;
    /** Total chips committed across all streets this hand (drives side-pot math). */
    public int totalCommitted;
    public boolean folded;
    public boolean allIn;
    /** Dealt into the current hand (has chips and is sitting in). */
    public boolean inHand;
    /** Has taken at least one voluntary action on the current street. */
    public boolean hasActed;
    /** After facing an incomplete all-in raise, a matched player may only call/fold. */
    public boolean restrictedToCall;

    public Player(String id, String name, int chips, boolean isBot) {
        this.id = id;
        this.name = name;
        this.chips = chips;
        this.isBot = isBot;
    }

    void resetForHand() {
        hole.clear();
        streetBet = 0;
        totalCommitted = 0;
        folded = false;
        allIn = false;
        hasActed = false;
        restrictedToCall = false;
        inHand = chips > 0;
    }

    void resetForStreet() {
        streetBet = 0;
        hasActed = false;
        restrictedToCall = false;
    }

    /** True while the player can still take actions (in the hand, not folded, has chips). */
    public boolean canAct() {
        return inHand && !folded && !allIn && chips > 0;
    }

    @Override
    public String toString() {
        return name + "(" + chips + ")";
    }
}
