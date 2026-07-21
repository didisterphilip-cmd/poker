package com.homepoker.engine;

/** A player decision. {@code amount} is the total street bet to reach for BET/RAISE. */
public final class Action {
    public enum Type { FOLD, CHECK, CALL, BET, RAISE, ALL_IN }

    public final Type type;
    /** For BET/RAISE: the target total street contribution. Ignored otherwise. */
    public final int amount;

    public Action(Type type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public static Action fold()            { return new Action(Type.FOLD, 0); }
    public static Action check()           { return new Action(Type.CHECK, 0); }
    public static Action call()            { return new Action(Type.CALL, 0); }
    public static Action bet(int to)       { return new Action(Type.BET, to); }
    public static Action raiseTo(int to)   { return new Action(Type.RAISE, to); }
    public static Action allIn()           { return new Action(Type.ALL_IN, 0); }

    @Override
    public String toString() {
        return type + (type == Type.BET || type == Type.RAISE ? "(" + amount + ")" : "");
    }
}
