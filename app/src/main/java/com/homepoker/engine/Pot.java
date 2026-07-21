package com.homepoker.engine;

import java.util.ArrayList;
import java.util.List;

/** A (main or side) pot: an amount plus the players eligible to win it. */
public final class Pot {
    public int amount;
    public final List<Player> eligible = new ArrayList<>();

    public Pot(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Pot(" + amount + ") [");
        for (int i = 0; i < eligible.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(eligible.get(i).name);
        }
        return sb.append(']').toString();
    }
}
