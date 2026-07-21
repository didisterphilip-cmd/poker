package com.homepoker.game

import com.homepoker.engine.Action
import com.homepoker.engine.Card

/** An immutable snapshot the UI renders. Rebuilt after every engine change. */
data class SeatUi(
    val name: String,
    val chips: Int,
    val streetBet: Int,
    val hole: List<Card>,
    val revealHole: Boolean,
    val folded: Boolean,
    val allIn: Boolean,
    val isHuman: Boolean,
    val isButton: Boolean,
    val isDealerBlind: String?, // "SB" / "BB" / null
    val isActing: Boolean,
    val isWinner: Boolean,
    val lastAction: String?,
    val emote: String?,
)

enum class Difficulty { EASY, MEDIUM, HARD }

data class TableState(
    val seats: List<SeatUi> = emptyList(),
    val board: List<Card> = emptyList(),
    val pot: Int = 0,
    val phaseLabel: String = "",
    val message: String = "Tap New Hand to deal",
    val handInProgress: Boolean = false,
    val humansTurn: Boolean = false,
    val legal: Set<Action.Type> = emptySet(),
    val toCall: Int = 0,
    val minRaiseTo: Int = 0,
    val maxRaiseTo: Int = 0,
    val bigBlind: Int = 10,
    val turnFraction: Float = 1f,     // remaining turn time, 1..0
    val handHistory: List<String> = emptyList(),
    val showdownActive: Boolean = false,
)
