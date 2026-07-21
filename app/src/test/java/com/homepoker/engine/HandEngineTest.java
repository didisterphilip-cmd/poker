package com.homepoker.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Betting flow, blinds, side pots, split pots, min-raise and uncalled-bet rules. */
class HandEngineTest {

    /** Build a deck matching the engine's deal order so hands are deterministic. */
    private Deck stacked(List<Player> seats, int button, List<Card[]> holes, List<Card> board) {
        int n = seats.size();
        List<Integer> order = new ArrayList<>();
        for (int step = 1; step <= n; step++) order.add((button + step) % n);
        List<Card> deck = new ArrayList<>();
        for (int round = 0; round < 2; round++)
            for (int seat : order) deck.add(holes.get(seat)[round]);
        deck.addAll(board);
        return Deck.stacked(deck);
    }

    private int total(List<Player> ps) { int t = 0; for (Player p : ps) t += p.chips; return t; }

    @Test void headsUpBestHandWins() {
        Player p0 = new Player("0", "P0", 1000, true);
        Player p1 = new Player("1", "P1", 1000, true);
        List<Player> seats = Arrays.asList(p0, p1);
        Deck deck = stacked(seats, 0, Arrays.asList(
                new Card[]{Card.of("As"), Card.of("Ah")},
                new Card[]{Card.of("Kd"), Card.of("Kc")}), Card.list("2c 7d 9s Jh 3s"));
        HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, 0, deck);
        e.startHand();
        assertSame(p0, e.currentActor());
        e.act(Action.call()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        assertTrue(e.isComplete());
        assertEquals(1010, p0.chips);
        assertEquals(990, p1.chips);
        assertEquals(2000, total(seats));
    }

    @Test void everyoneFoldsToBigBlind() {
        Player p0 = new Player("0", "P0", 1000, true);
        Player p1 = new Player("1", "P1", 1000, true);
        Player p2 = new Player("2", "P2", 1000, true);
        List<Player> seats = Arrays.asList(p0, p1, p2);
        Deck deck = stacked(seats, 0, Arrays.asList(
                new Card[]{Card.of("2c"), Card.of("7d")},
                new Card[]{Card.of("3c"), Card.of("8d")},
                new Card[]{Card.of("4c"), Card.of("9d")}), Card.list("Ts Js Qs Ks Ah"));
        HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, 0, deck);
        e.startHand();
        e.act(Action.fold()); // P0 UTG
        e.act(Action.fold()); // P1 SB
        assertTrue(e.isComplete());
        assertEquals(1005, p2.chips);
        assertEquals(3000, total(seats));
    }

    @Test void sidePotWithShortAllIn() {
        Player p0 = new Player("0", "P0", 100, true);
        Player p1 = new Player("1", "P1", 100, true);
        Player p2 = new Player("2", "P2", 40, true);
        List<Player> seats = Arrays.asList(p0, p1, p2);
        Deck deck = stacked(seats, 0, Arrays.asList(
                new Card[]{Card.of("Kc"), Card.of("Kd")},
                new Card[]{Card.of("Qc"), Card.of("Qd")},
                new Card[]{Card.of("Ac"), Card.of("Ad")}), Card.list("2h 7s 9d Jh 3c"));
        HandEngine e = new HandEngine(TableConfig.cash(5, 10, 100), seats, 0, deck);
        e.startHand();
        e.act(Action.allIn()); e.act(Action.allIn()); e.act(Action.allIn());
        assertTrue(e.isComplete());
        assertEquals(120, p2.chips); // short stack wins main pot
        assertEquals(120, p0.chips); // kings win side pot
        assertEquals(0, p1.chips);   // queens win nothing
        assertEquals(240, total(seats));
        assertEquals(2, e.result().awards.size());
    }

    @Test void splitPotReturnsStacks() {
        Player p0 = new Player("0", "P0", 1000, true);
        Player p1 = new Player("1", "P1", 1000, true);
        List<Player> seats = Arrays.asList(p0, p1);
        Deck deck = stacked(seats, 0, Arrays.asList(
                new Card[]{Card.of("Ac"), Card.of("Kc")},
                new Card[]{Card.of("Ad"), Card.of("Kd")}), Card.list("2h 7s 9d Jh 3s"));
        HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, 0, deck);
        e.startHand();
        e.act(Action.call()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        e.act(Action.check()); e.act(Action.check());
        assertEquals(1000, p0.chips);
        assertEquals(1000, p1.chips);
    }

    @Test void minRaiseIsEnforced() {
        Player p0 = new Player("0", "P0", 1000, true);
        Player p1 = new Player("1", "P1", 1000, true);
        List<Player> seats = Arrays.asList(p0, p1);
        Deck deck = stacked(seats, 0, Arrays.asList(
                new Card[]{Card.of("Ac"), Card.of("Kc")},
                new Card[]{Card.of("Ad"), Card.of("Kd")}), Card.list("2h 7s 9d Jh 3s"));
        HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, 0, deck);
        e.startHand();
        assertEquals(20, e.minRaiseTo());
        assertThrows(IllegalArgumentException.class, () -> e.act(Action.raiseTo(15)));
        e.act(Action.raiseTo(30));
        assertEquals(50, e.minRaiseTo());
    }
}
