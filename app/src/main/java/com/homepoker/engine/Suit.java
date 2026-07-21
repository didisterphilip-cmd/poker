package com.homepoker.engine;

/** The four card suits. Order is irrelevant to hand strength (poker suits are equal). */
public enum Suit {
    CLUBS('c', "♣"),
    DIAMONDS('d', "♦"),
    HEARTS('h', "♥"),
    SPADES('s', "♠");

    public final char code;
    public final String glyph;

    Suit(char code, String glyph) {
        this.code = code;
        this.glyph = glyph;
    }

    public static Suit fromCode(char c) {
        for (Suit s : values()) {
            if (s.code == Character.toLowerCase(c)) return s;
        }
        throw new IllegalArgumentException("Unknown suit code: " + c);
    }
}
