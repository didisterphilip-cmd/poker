package com.homepoker.engine;

/** The nine poker hand categories, ordered weakest (0) to strongest (8). */
public enum HandCategory {
    HIGH_CARD("High Card"),
    PAIR("Pair"),
    TWO_PAIR("Two Pair"),
    THREE_OF_A_KIND("Three of a Kind"),
    STRAIGHT("Straight"),
    FLUSH("Flush"),
    FULL_HOUSE("Full House"),
    FOUR_OF_A_KIND("Four of a Kind"),
    STRAIGHT_FLUSH("Straight Flush"); // includes the Royal Flush (ace-high straight flush)

    public final String label;

    HandCategory(String label) {
        this.label = label;
    }
}
