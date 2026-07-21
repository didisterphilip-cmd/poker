package com.homepoker.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exhaustive checks on hand classification, category ordering, kickers and splits. */
class HandEvaluatorTest {

    private HandValue eval(String cards) {
        return HandEvaluator.best(Card.list(cards));
    }

    private int cmp(String a, String b) {
        return eval(a).compareTo(eval(b));
    }

    @Test void classifiesCategories() {
        assertTrue(eval("As Ks Qs Js Ts 2c 3d").isRoyalFlush());
        assertEquals(HandCategory.STRAIGHT_FLUSH, eval("9s 8s 7s 6s 5s Ac Kd").category);
        assertEquals(HandCategory.FOUR_OF_A_KIND, eval("As Ad Ac Ah Kd 2c 3s").category);
        assertEquals(HandCategory.FULL_HOUSE, eval("As Ad Ac Kh Kd 2c 3s").category);
        assertEquals(HandCategory.FLUSH, eval("As Js 9s 6s 3s Kc Qd").category);
        assertEquals(HandCategory.STRAIGHT, eval("Ts 9d 8c 7h 6s Ac 2d").category);
        assertEquals(HandCategory.THREE_OF_A_KIND, eval("As Ad Ac Kh Qd 2c 3s").category);
        assertEquals(HandCategory.TWO_PAIR, eval("As Ad Kc Kh Qd 2c 3s").category);
        assertEquals(HandCategory.PAIR, eval("As Ad Kc Qh Jd 2c 3s").category);
        assertEquals(HandCategory.HIGH_CARD, eval("As Kd Qc Jh 9d 2c 3s").category);
    }

    @Test void wheelIsFiveHighStraight() {
        HandValue v = eval("Ah 2d 3c 4s 5h Kc Qd");
        assertEquals(HandCategory.STRAIGHT, v.category);
        assertEquals(5, v.tiebreak[0]);
    }

    @Test void categoryOrdering() {
        assertTrue(cmp("9s 8s 7s 6s 5s 2c 3d", "As Ad Ac Ah Kd 2c 4s") > 0); // SF > quads
        assertTrue(cmp("As Ad Ac Ah Kd 2c 4s", "Ks Kd Kc Qh Qd 2c 4s") > 0); // quads > boat
        assertTrue(cmp("Ks Kd Kc Qh Qd 2c 4s", "As Js 9s 6s 3s Kc Qd") > 0); // boat > flush
        assertTrue(cmp("As Js 9s 6s 3s Kc Qd", "Ts 9d 8c 7h 6s Ac 2d") > 0); // flush > straight
        assertTrue(cmp("Ts 9d 8c 7h 6s Ac 2d", "As Ad Ac Kh Qd 2c 4s") > 0); // straight > trips
        assertTrue(cmp("As Ad Ac Kh Qd 2c 4s", "As Ad Kc Kh Qd 2c 4s") > 0); // trips > two pair
        assertTrue(cmp("As Ad Kc Kh Qd 2c 4s", "As Ad Kc Qh Jd 2c 4s") > 0); // two pair > pair
        assertTrue(cmp("As Ad Kc Qh Jd 2c 4s", "As Kd Qc Jh 9d 2c 4s") > 0); // pair > high
    }

    @Test void kickersAndTies() {
        assertTrue(cmp("As Ad Kc Qh Jd 2c 4s", "As Ad Kc Qh Td 2c 4s") > 0);   // J kicker > T kicker
        assertEquals(0, cmp("As Ad Kc Qh Jd 2c 4s", "Ah Ac Kd Qs Jh 2d 4h"));   // identical -> split
        assertTrue(cmp("As Kd Qc Jh Ts 2c 4s", "Ah 2d 3c 4s 5h Kc 4d") > 0);    // broadway > wheel
        assertEquals(0, cmp("Ts 9d 8c 7h 6s 2c 3d", "Th 9s 8d 7c 6h 2s 3h"));   // board straight ties
    }

    @Test void bestOfSevenPicksStrongestFive() {
        assertTrue(cmp("Ks 2s 9s 8s 7s 6s 4d", "Qs 2s 9s 8s 7s 6s 4d") > 0); // higher flush card wins
    }
}
