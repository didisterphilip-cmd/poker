package com.homepoker.ai;

import com.homepoker.engine.Action;
import com.homepoker.engine.HandEngine;
import com.homepoker.engine.Player;

import java.util.List;
import java.util.Random;

/**
 * An equity-driven opponent. It estimates its win probability with a Monte-Carlo
 * roll-out, compares that to the pot odds it is being offered, and then shapes the
 * final decision through its {@link BotProfile} (tightness, aggression, bluffing).
 *
 * <p>The result is opponents that fold weak hands, value-bet strong ones, call with
 * the right odds, and occasionally bluff — distinct personalities, not one script.
 */
public final class PokerBot {
    public final BotProfile profile;
    private final Random rng;

    public PokerBot(BotProfile profile, Random rng) {
        this.profile = profile;
        this.rng = rng;
    }

    public PokerBot(BotProfile profile) {
        this(profile, new Random());
    }

    public Action decide(HandEngine e, Player me) {
        List<Action.Type> legal = e.legalActions(me);
        if (legal.isEmpty()) return null;

        int toCall = e.amountToCall(me);
        int pot = e.potTotal();
        int opponents = liveOpponents(e, me);
        int sims = e.board.isEmpty() ? 180 : 260;
        double equity = HandStrength.equity(me.hole, e.board, opponents, sims, rng);

        double jitter = profile.noise * (rng.nextDouble() - 0.5);
        double callTh = clamp01(profile.callThreshold + jitter);
        double raiseTh = clamp01(profile.raiseThreshold + jitter);

        boolean canRaise = legal.contains(Action.Type.BET) || legal.contains(Action.Type.RAISE);
        boolean canCheck = legal.contains(Action.Type.CHECK);

        // 1) Strong hand -> raise for value (sometimes just call to trap when very strong).
        if (canRaise && equity >= raiseTh) {
            if (equity > 0.85 && rng.nextDouble() < 0.25 && toCall > 0) {
                return Action.call(); // slow-play a monster occasionally
            }
            return sizedRaise(e, me, pot);
        }

        // 2) Bluff: fire with a weak hand now and then, mostly heads-up / when checked to.
        if (canRaise && equity < raiseTh && opponents <= 2 && rng.nextDouble() < profile.bluffFrequency) {
            return sizedRaise(e, me, pot);
        }

        // 3) Nothing to call: take a free card (occasionally a small probe bet).
        if (toCall == 0) {
            if (canRaise && equity > 0.5 && rng.nextDouble() < 0.35) {
                return sizedRaise(e, me, pot);
            }
            return canCheck ? Action.check() : Action.call();
        }

        // 4) Facing a bet: continue only with the right odds and enough equity.
        double potOdds = toCall / (double) (pot + toCall);
        if (equity >= potOdds && equity >= callTh) {
            return Action.call();
        }
        return Action.fold();
    }

    /** Build a legal bet/raise sized off the pot, clamped to the bot's stack. */
    private Action sizedRaise(HandEngine e, Player me, int pot) {
        List<Action.Type> legal = e.legalActions(me);
        boolean isBet = legal.contains(Action.Type.BET); // no wager yet this street
        int maxTo = e.maxRaiseTo(me);

        if (isBet) {
            // Opening bet: a fraction of the pot, at least one big blind.
            int target = Math.max(e.config.bigBlind, (int) Math.round(pot * profile.raiseSizing));
            if (target >= maxTo) return shoveOrCall(e, me, legal);
            return Action.bet(target);
        }

        // Raising over an existing bet.
        int minTo = e.minRaiseTo();
        if (maxTo < minTo || !legal.contains(Action.Type.RAISE)) {
            return shoveOrCall(e, me, legal);
        }
        // Target = current bet plus a pot-fraction increment, floored at the min raise.
        int toMatch = e.betToMatch();
        int increment = Math.max(minTo - toMatch, (int) Math.round(pot * profile.raiseSizing));
        int target = Math.min(maxTo, Math.max(minTo, toMatch + increment));
        if (target >= maxTo) return shoveOrCall(e, me, legal);
        return Action.raiseTo(target);
    }

    private Action shoveOrCall(HandEngine e, Player me, List<Action.Type> legal) {
        if (legal.contains(Action.Type.ALL_IN)) return Action.allIn();
        return e.amountToCall(me) == 0 ? Action.check() : Action.call();
    }

    private static int liveOpponents(HandEngine e, Player me) {
        int c = 0;
        for (Player p : e.seats) if (p != me && p.inHand && !p.folded) c++;
        return c;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
