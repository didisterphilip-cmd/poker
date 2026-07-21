package com.homepoker.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An immutable playing card. */
public final class Card {
    public final Rank rank;
    public final Suit suit;

    public Card(Rank rank, Suit suit) {
        this.rank = Objects.requireNonNull(rank);
        this.suit = Objects.requireNonNull(suit);
    }

    /** Parse a two-character notation such as "As", "Td", "2c". */
    public static Card of(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Card notation must be 2 chars, got: " + notation);
        }
        return new Card(Rank.fromCode(notation.charAt(0)), Suit.fromCode(notation.charAt(1)));
    }

    /** Parse a space-separated list, e.g. "As Ks Qs Js Ts". */
    public static List<Card> list(String cards) {
        List<Card> out = new ArrayList<>();
        for (String tok : cards.trim().split("\\s+")) {
            if (!tok.isEmpty()) out.add(of(tok));
        }
        return out;
    }

    public String notation() {
        return "" + rank.code + suit.code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card c = (Card) o;
        return rank == c.rank && suit == c.suit;
    }

    @Override
    public int hashCode() {
        return rank.ordinal() * 4 + suit.ordinal();
    }

    @Override
    public String toString() {
        return rank.code + suit.glyph;
    }
}
