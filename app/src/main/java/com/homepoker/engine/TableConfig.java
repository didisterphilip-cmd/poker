package com.homepoker.engine;

/** Immutable table settings for a hand / session. */
public final class TableConfig {
    public final int smallBlind;
    public final int bigBlind;
    public final int ante;           // per-player ante, 0 for none
    public final int startingStack;  // informational (used when creating players)

    public TableConfig(int smallBlind, int bigBlind, int ante, int startingStack) {
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.ante = ante;
        this.startingStack = startingStack;
    }

    public static TableConfig cash(int smallBlind, int bigBlind, int startingStack) {
        return new TableConfig(smallBlind, bigBlind, 0, startingStack);
    }
}
