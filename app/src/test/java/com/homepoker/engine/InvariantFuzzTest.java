package com.homepoker.engine;

import com.homepoker.ai.BotProfile;
import com.homepoker.ai.PokerBot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based fuzzing: play thousands of hands with random legal actions and
 * with real bots, asserting the invariants that must always hold — chips are
 * conserved, no stack goes negative, and no illegal state is ever reached.
 */
class InvariantFuzzTest {

    private int total(List<Player> ps) { int t = 0; for (Player p : ps) t += p.chips; return t; }
    private int withChips(List<Player> ps) { int c = 0; for (Player p : ps) if (p.chips > 0) c++; return c; }

    @Test void randomActionsConserveChips() {
        Random rng = new Random(12345);
        int handsPlayed = 0;
        for (int trial = 0; trial < 1500; trial++) {
            int n = 2 + rng.nextInt(8);
            List<Player> seats = new ArrayList<>();
            for (int i = 0; i < n; i++) seats.add(new Player("" + i, "P" + i, 500 + rng.nextInt(1500), true));
            int startTotal = total(seats);
            int button = rng.nextInt(n);
            int hands = 0;
            while (withChips(seats) >= 2 && hands < 150) {
                HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, button,
                        new Deck(new Random(rng.nextLong())));
                e.startHand();
                int guard = 0;
                while (!e.isComplete()) {
                    Player actor = e.currentActor();
                    if (actor == null) break;
                    List<Action.Type> legal = e.legalActions(actor);
                    assertFalse(legal.isEmpty(), "actor must have a legal action");
                    e.act(randomAction(e, actor, legal, rng));
                    assertTrue(++guard <= 500, "betting round should terminate");
                }
                for (Player p : seats) assertTrue(p.chips >= 0, "no negative stacks");
                assertEquals(startTotal, total(seats), "chips must be conserved");
                button = (button + 1) % n;
                hands++;
                handsPlayed++;
            }
        }
        assertTrue(handsPlayed > 1000, "fuzz should exercise many hands");
    }

    @Test void botsPlayCleanlyAndSkillMatters() {
        Random rng = new Random(7);
        long taChips = 0, lpChips = 0;
        for (int table = 0; table < 30; table++) {
            List<Player> seats = new ArrayList<>();
            List<PokerBot> bots = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                seats.add(new Player("" + i, "P" + i, 1000, true));
                bots.add(new PokerBot(i % 2 == 0 ? BotProfile.tightAggressive() : BotProfile.loosePassive(),
                        new Random(rng.nextLong())));
            }
            int start = total(seats);
            int button = 0, hands = 0;
            while (withChips(seats) >= 2 && hands < 50) {
                HandEngine e = new HandEngine(TableConfig.cash(5, 10, 1000), seats, button,
                        new Deck(new Random(rng.nextLong())));
                e.startHand();
                int guard = 0;
                while (!e.isComplete()) {
                    Player actor = e.currentActor();
                    if (actor == null) break;
                    Action act = bots.get(seats.indexOf(actor)).decide(e, actor);
                    assertNotNull(act);
                    e.act(act);
                    assertTrue(++guard <= 400);
                }
                assertEquals(start, total(seats));
                button = (button + 1) % 6;
                hands++;
            }
            for (int i = 0; i < 6; i++) { if (i % 2 == 0) taChips += seats.get(i).chips; else lpChips += seats.get(i).chips; }
        }
        assertTrue(taChips > lpChips, "tight-aggressive should beat loose-passive over a sample");
    }

    private Action randomAction(HandEngine e, Player actor, List<Action.Type> legal, Random rng) {
        Action.Type t = legal.get(rng.nextInt(legal.size()));
        switch (t) {
            case FOLD: return Action.fold();
            case CHECK: return Action.check();
            case CALL: return Action.call();
            case ALL_IN: return Action.allIn();
            case BET: {
                int min = e.config.bigBlind, max = e.maxRaiseTo(actor);
                return Action.bet(min + (max > min ? rng.nextInt(max - min + 1) : 0));
            }
            case RAISE: {
                int min = e.minRaiseTo(), max = e.maxRaiseTo(actor);
                if (max < min) return Action.allIn();
                return Action.raiseTo(min + rng.nextInt(max - min + 1));
            }
            default: return Action.fold();
        }
    }
}
