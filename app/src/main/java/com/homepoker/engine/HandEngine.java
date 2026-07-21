package com.homepoker.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A single-hand No-Limit Texas Hold'em state machine.
 *
 * <p>Responsibilities: post blinds, deal, enforce legal actions, run each
 * betting round (with correct min-raise and incomplete-all-in reopening rules),
 * advance streets, run the board out when players are all-in, and award the
 * pot(s) — including side pots, split pots, kickers, and odd-chip distribution.
 *
 * <p>The engine is deterministic given its {@link Deck}; the same seeded deck
 * and action sequence always yields the same result, which is what makes the
 * unit tests exhaustive and what lets a server replay/verify a hand.
 */
public final class HandEngine {

    public enum Phase { PREFLOP, FLOP, TURN, RIVER, SHOWDOWN, COMPLETE }

    public final TableConfig config;
    public final List<Player> seats;   // players seated for this hand, in seat order
    public final int buttonIndex;
    private final Deck deck;

    public final List<Card> board = new ArrayList<>(5);
    public Phase phase = Phase.PREFLOP;

    private int actorIndex = -1;
    private int toMatch;       // highest streetBet this round
    private int minRaise;      // minimum raise increment currently required
    private int lastAggressor = -1;
    private int sbIndex, bbIndex;

    private HandResult result;
    public final List<String> log = new ArrayList<>();

    public HandEngine(TableConfig config, List<Player> seats, int buttonIndex, Deck deck) {
        this.config = config;
        this.seats = seats;
        this.buttonIndex = buttonIndex;
        this.deck = deck;
    }

    // ---------------------------------------------------------------- setup

    public void startHand() {
        deck.shuffle();
        for (Player p : seats) p.resetForHand();
        board.clear();
        phase = Phase.PREFLOP;
        result = null;

        int inHand = countInHand();
        if (inHand < 2) throw new IllegalStateException("Need >=2 players with chips");

        // Blind seats.
        if (inHand == 2) {
            sbIndex = buttonIndex;                 // heads-up: button is the small blind
            bbIndex = nextInHand(buttonIndex);
        } else {
            sbIndex = nextInHand(buttonIndex);
            bbIndex = nextInHand(sbIndex);
        }

        // Antes first (if any).
        if (config.ante > 0) {
            for (Player p : seats) if (p.inHand) commit(p, config.ante);
        }

        // Post blinds.
        commit(seats.get(sbIndex), config.smallBlind);
        commit(seats.get(bbIndex), config.bigBlind);
        toMatch = config.bigBlind;
        minRaise = config.bigBlind;
        lastAggressor = bbIndex;
        // Blind posters have not voluntarily acted; they keep their option.
        seats.get(sbIndex).hasActed = false;
        seats.get(bbIndex).hasActed = false;

        // Deal two hole cards to each player in the hand (one at a time, like a real deal).
        for (int round = 0; round < 2; round++) {
            int i = nextInHand(buttonIndex);
            for (int c = 0; c < inHand; c++) {
                seats.get(i).hole.add(deck.draw());
                i = nextInHand(i);
            }
        }

        // First to act preflop.
        actorIndex = (inHand == 2) ? sbIndex : nextInHand(bbIndex);
        actorIndex = ensureCanAct(actorIndex);
        log.add("Hand start: button=" + seats.get(buttonIndex).name
                + " SB=" + seats.get(sbIndex).name + " BB=" + seats.get(bbIndex).name);

        // Blinds may already have everyone all-in (short stacks): resolve if so.
        if (isBettingRoundComplete() || countActiveNotFolded() <= 1) {
            advanceStreet();
        }
    }

    // ---------------------------------------------------------------- queries

    public Player currentActor() {
        return (phase == Phase.SHOWDOWN || phase == Phase.COMPLETE || actorIndex < 0)
                ? null : seats.get(actorIndex);
    }

    public boolean isComplete() { return phase == Phase.COMPLETE; }

    /** Total chips currently in all pots (sum of everyone's committed chips). */
    public int potTotal() {
        int t = 0;
        for (Player p : seats) t += p.totalCommitted;
        return t;
    }

    /** Chips {@code p} must add to call the current bet (capped by their stack). */
    public int amountToCall(Player p) {
        return Math.min(toMatch - p.streetBet, p.chips);
    }

    /** The highest street contribution any player must match to stay in. */
    public int betToMatch() {
        return toMatch;
    }

    /** The minimum total street contribution a legal raise must reach. */
    public int minRaiseTo() {
        return toMatch + minRaise;
    }

    /** The maximum a player can raise to (their all-in). */
    public int maxRaiseTo(Player p) {
        return p.streetBet + p.chips;
    }

    public List<Action.Type> legalActions(Player p) {
        List<Action.Type> out = new ArrayList<>();
        if (p != currentActor() || !p.canAct()) return out;
        int call = amountToCall(p);
        out.add(Action.Type.FOLD);
        if (call == 0) {
            out.add(Action.Type.CHECK);
        } else {
            out.add(Action.Type.CALL);
        }
        // Can the player put in more chips than a call (i.e. bet or raise)?
        boolean hasRaiseChips = p.chips > call;
        boolean raisingAllowed = !p.restrictedToCall && hasRaiseChips;
        if (raisingAllowed) {
            out.add(toMatch == 0 ? Action.Type.BET : Action.Type.RAISE);
        }
        // All-in is always available while the player has chips.
        if (p.chips > 0) out.add(Action.Type.ALL_IN);
        return out;
    }

    // ---------------------------------------------------------------- acting

    public void act(Action action) {
        Player p = currentActor();
        if (p == null) throw new IllegalStateException("No actor (phase=" + phase + ")");
        if (!p.canAct()) throw new IllegalStateException(p.name + " cannot act");

        switch (action.type) {
            case FOLD:
                p.folded = true;
                p.hasActed = true;
                log.add(p.name + " folds");
                break;
            case CHECK:
                if (amountToCall(p) != 0) throw new IllegalArgumentException("Cannot check facing a bet");
                p.hasActed = true;
                log.add(p.name + " checks");
                break;
            case CALL: {
                int call = amountToCall(p);
                commit(p, call);
                p.hasActed = true;
                log.add(p.name + " calls " + call);
                break;
            }
            case BET:
            case RAISE: {
                applyAggressive(p, action.amount);
                break;
            }
            case ALL_IN: {
                int allInTo = p.streetBet + p.chips;
                if (allInTo > toMatch) {
                    applyAggressive(p, allInTo); // all-in raise (possibly incomplete)
                } else {
                    // All-in call (or under-call): just push remaining chips.
                    commit(p, p.chips);
                    p.hasActed = true;
                    log.add(p.name + " is all-in for " + p.streetBet);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown action " + action.type);
        }

        advanceAfterAction();
    }

    private void applyAggressive(Player p, int targetTo) {
        int maxTo = maxRaiseTo(p);
        boolean isAllIn = targetTo >= maxTo;
        if (isAllIn) targetTo = maxTo;

        int minTo = minRaiseTo();
        if (!isAllIn) {
            if (toMatch == 0) {
                if (targetTo < config.bigBlind)
                    throw new IllegalArgumentException("Bet must be at least the big blind");
            } else {
                if (p.restrictedToCall)
                    throw new IllegalArgumentException("Betting is not reopened for this player");
                if (targetTo < minTo)
                    throw new IllegalArgumentException("Raise must reach at least " + minTo);
            }
        }

        int oldToMatch = toMatch;
        int add = targetTo - p.streetBet;
        commit(p, add);
        int increment = targetTo - oldToMatch;

        boolean fullRaise = increment >= minRaise || oldToMatch == 0;
        if (targetTo > oldToMatch) {
            toMatch = targetTo;
            if (fullRaise) {
                minRaise = Math.max(minRaise, increment);
                lastAggressor = actorIndex;
                // A full raise/bet reopens the action for everyone else.
                for (Player o : seats) {
                    if (o != p && o.canAct()) { o.hasActed = false; o.restrictedToCall = false; }
                }
            } else {
                // Incomplete all-in raise: players who had already matched may now
                // only call the extra (not re-raise); un-acted players keep full rights.
                for (Player o : seats) {
                    if (o != p && o.canAct()) {
                        o.hasActed = false;
                        if (o.streetBet >= oldToMatch) o.restrictedToCall = true;
                    }
                }
            }
            lastAggressor = actorIndex;
        }
        p.hasActed = true;
        p.restrictedToCall = false;
        log.add(p.name + (oldToMatch == 0 ? " bets " : " raises to ") + targetTo
                + (isAllIn ? " (all-in)" : ""));
    }

    private void advanceAfterAction() {
        if (countActiveNotFolded() <= 1) { endHandByFold(); return; }
        if (isBettingRoundComplete()) { advanceStreet(); return; }
        int next = nextCanAct(actorIndex);
        if (next < 0) { advanceStreet(); return; }
        actorIndex = next;
    }

    // ---------------------------------------------------------------- streets

    private void advanceStreet() {
        // Return any uncalled bet (chips one player wagered beyond what anyone matched).
        returnUncalledBet();
        // Reset street state.
        for (Player p : seats) p.resetForStreet();
        toMatch = 0;
        minRaise = config.bigBlind;
        lastAggressor = -1;

        switch (phase) {
            case PREFLOP:
                dealBoard(3); phase = Phase.FLOP; break;
            case FLOP:
                dealBoard(1); phase = Phase.TURN; break;
            case TURN:
                dealBoard(1); phase = Phase.RIVER; break;
            case RIVER:
                phase = Phase.SHOWDOWN; showdown(); return;
            default:
                return;
        }
        log.add(phase + ": " + board);

        // If fewer than two players can act, no betting this street: run it out.
        if (countCanAct() < 2 || countActiveNotFolded() <= 1) {
            if (countActiveNotFolded() <= 1) { endHandByFold(); return; }
            advanceStreet();
            return;
        }
        // First to act post-flop is first live player left of the button.
        actorIndex = ensureCanAct(nextInHand(buttonIndex));
    }

    private void dealBoard(int n) {
        for (int i = 0; i < n; i++) board.add(deck.draw());
    }

    // ---------------------------------------------------------------- settlement

    private void endHandByFold() {
        returnUncalledBet();
        phase = Phase.COMPLETE;
        result = new HandResult();
        result.board.addAll(board);
        result.wonByFold = true;
        Player winner = null;
        for (Player p : seats) if (p.inHand && !p.folded) { winner = p; break; }
        int pot = potTotal();
        winner.chips += pot;
        result.addWinnings(winner, pot);
        HandResult.Award a = new HandResult.Award(pot);
        a.winners.add(winner);
        a.payout.put(winner, pot);
        result.awards.add(a);
        log.add(winner.name + " wins " + pot + " (all others folded)");
    }

    private void showdown() {
        phase = Phase.COMPLETE;
        result = new HandResult();
        result.board.addAll(board);

        // Evaluate each contender's best hand.
        List<Player> contenders = new ArrayList<>();
        for (Player p : seats) if (p.inHand && !p.folded) contenders.add(p);

        List<Pot> pots = SidePots.build(seats);
        for (Pot pot : pots) {
            // Winners = eligible players with the best hand value.
            HandValue best = null;
            List<Player> winners = new ArrayList<>();
            HandResult.Award award = new HandResult.Award(pot.amount);
            for (Player p : pot.eligible) {
                HandValue v = HandEvaluator.best(new ArrayList<>(p.hole), board);
                int cmp = (best == null) ? 1 : v.compareTo(best);
                if (best == null || cmp > 0) {
                    best = v;
                    winners.clear();
                    winners.add(p);
                } else if (cmp == 0) {
                    winners.add(p);
                }
            }
            award.winningHand = best;
            award.winners.addAll(winners);
            distribute(pot.amount, winners, award);
            result.awards.add(award);
        }
        StringBuilder sb = new StringBuilder("Showdown on " + board + ": ");
        for (HandResult.Award a : result.awards) {
            sb.append(a.potAmount).append("->");
            for (Player w : a.winners) sb.append(w.name).append(' ');
            sb.append('(').append(a.winningHand != null ? a.winningHand.describe() : "?").append(") ");
        }
        log.add(sb.toString());
    }

    /**
     * Split {@code amount} among {@code winners}, awarding odd chips to the
     * winner(s) in earliest position left of the button (standard rule).
     */
    private void distribute(int amount, List<Player> winners, HandResult.Award award) {
        int n = winners.size();
        int base = amount / n;
        int remainder = amount - base * n;
        for (Player w : winners) {
            w.chips += base;
            award.payout.merge(w, base, Integer::sum);
            result.addWinnings(w, base);
        }
        if (remainder > 0) {
            // Order winners by seat position starting left of the button.
            List<Player> order = new ArrayList<>(winners);
            order.sort(Comparator.comparingInt(this::seatDistanceFromButton));
            for (int i = 0; i < remainder; i++) {
                Player w = order.get(i % order.size());
                w.chips += 1;
                award.payout.merge(w, 1, Integer::sum);
                result.addWinnings(w, 1);
            }
        }
    }

    private int seatDistanceFromButton(Player p) {
        int idx = seats.indexOf(p);
        int dist = idx - buttonIndex;
        if (dist <= 0) dist += seats.size();
        return dist;
    }

    public HandResult result() { return result; }

    // ---------------------------------------------------------------- helpers

    /**
     * If exactly one player put more chips into this street than anyone else
     * matched, the excess is uncalled and returned to that player (standard rule).
     */
    private void returnUncalledBet() {
        Player top = null;
        int h1 = 0, h2 = 0;
        int topCount = 0;
        for (Player p : seats) {
            if (!p.inHand) continue;
            if (p.streetBet > h1) { h2 = h1; h1 = p.streetBet; top = p; topCount = 1; }
            else if (p.streetBet == h1) { topCount++; }
            else if (p.streetBet > h2) { h2 = p.streetBet; }
        }
        if (top != null && topCount == 1 && h1 > h2) {
            int refund = h1 - h2;
            top.chips += refund;
            top.streetBet -= refund;
            top.totalCommitted -= refund;
            if (top.chips > 0) top.allIn = false;
            log.add(top.name + " gets back uncalled " + refund);
        }
    }

    private void commit(Player p, int amount) {
        int a = Math.min(amount, p.chips);
        p.chips -= a;
        p.streetBet += a;
        p.totalCommitted += a;
        if (p.chips == 0) p.allIn = true;
    }

    private boolean isBettingRoundComplete() {
        int active = 0;
        for (Player p : seats) {
            if (!p.inHand || p.folded) continue;
            active++;
            if (p.allIn) continue;
            if (!p.hasActed) return false;
            if (p.streetBet != toMatch) return false;
        }
        return active >= 1;
    }

    private int countInHand() {
        int c = 0; for (Player p : seats) if (p.chips > 0 && !p.folded) c++; return c;
    }
    private int countActiveNotFolded() {
        int c = 0; for (Player p : seats) if (p.inHand && !p.folded) c++; return c;
    }
    private int countCanAct() {
        int c = 0; for (Player p : seats) if (p.canAct()) c++; return c;
    }

    private int nextInHand(int from) {
        int n = seats.size();
        for (int step = 1; step <= n; step++) {
            int i = (from + step) % n;
            if (seats.get(i).inHand) return i;
        }
        return from;
    }

    private int nextCanAct(int from) {
        int n = seats.size();
        for (int step = 1; step <= n; step++) {
            int i = (from + step) % n;
            if (seats.get(i).canAct() && !(seats.get(i).hasActed && seats.get(i).streetBet == toMatch)) {
                return i;
            }
        }
        return -1;
    }

    private int ensureCanAct(int idx) {
        if (seats.get(idx).canAct()) return idx;
        int n = nextCanAct(idx == 0 ? seats.size() - 1 : idx - 1);
        return n;
    }
}
