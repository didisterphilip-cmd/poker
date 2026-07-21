package com.homepoker.engine;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A 52-card deck with a cryptographically-secure Fisher-Yates shuffle.
 *
 * <p>By default the deck draws entropy from {@link SecureRandom} so that card
 * order is unpredictable and unbiased. Tests may inject a seeded {@link Random}
 * for reproducibility. In multiplayer the shuffle is performed server-side only
 * (see ARCHITECTURE.md) so clients can never influence or predict the order.
 */
public final class Deck {
    private final List<Card> cards = new ArrayList<>(52);
    private final Random rng;
    private int index = 0;
    private boolean preset = false;

    public Deck() {
        this(new SecureRandom());
    }

    /** Test / server hook: supply a deterministic RNG (e.g. seeded for replay). */
    public Deck(Random rng) {
        this.rng = rng;
        reset();
    }

    /**
     * Test helper: a deck whose draw order is exactly {@code order}. {@link #shuffle()}
     * becomes a no-op so scenarios can control every dealt card. Not for production use.
     */
    public static Deck stacked(List<Card> order) {
        Deck d = new Deck(new Random(0));
        d.cards.clear();
        d.cards.addAll(order);
        d.preset = true;
        d.index = 0;
        return d;
    }

    /** Rebuild the full ordered deck without shuffling. */
    public void reset() {
        cards.clear();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                cards.add(new Card(r, s));
            }
        }
        index = 0;
    }

    /**
     * Unbiased Fisher-Yates shuffle. Every one of the 52! permutations is
     * equally likely given a uniform RNG.
     */
    public void shuffle() {
        if (preset) { index = 0; return; }
        for (int i = cards.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Card tmp = cards.get(i);
            cards.set(i, cards.get(j));
            cards.set(j, tmp);
        }
        index = 0;
    }

    /** Draw the next card from the top of the deck. */
    public Card draw() {
        if (index >= cards.size()) {
            throw new IllegalStateException("Deck exhausted");
        }
        return cards.get(index++);
    }

    /** Cards remaining to be drawn. */
    public int remaining() {
        return cards.size() - index;
    }
}
