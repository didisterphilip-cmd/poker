package com.homepoker.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homepoker.ai.BotProfile
import com.homepoker.ai.PokerBot
import com.homepoker.engine.Action
import com.homepoker.engine.Deck
import com.homepoker.engine.HandEngine
import com.homepoker.engine.Player
import com.homepoker.engine.TableConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives an offline single-player table: the human sits in seat 0, the rest are
 * AI bots. The verified [HandEngine] owns all rules; this class only sequences
 * turns, animates bot "thinking" pauses, runs the turn timer, and projects state
 * into an immutable [TableState] for Compose. No game logic lives here.
 */
class GameViewModel : ViewModel() {

    private val config = TableConfig.cash(5, 10, 1000)
    private var seats: MutableList<Player> = mutableListOf()
    private var bots: MutableMap<String, PokerBot> = mutableMapOf()
    private var buttonIndex = 0
    private var engine: HandEngine? = null
    private var loopJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var difficulty = Difficulty.MEDIUM

    private val history = ArrayDeque<String>()   // most recent hands, capped
    private val lastActions = mutableMapOf<String, String>()
    private var winners: Set<String> = emptySet()

    private val _state = MutableStateFlow(TableState())
    val state: StateFlow<TableState> = _state.asStateFlow()

    private val turnTimerMs = 20_000L

    fun newGame(playerName: String, botCount: Int, difficulty: Difficulty) {
        this.difficulty = difficulty
        val profiles = when (difficulty) {
            Difficulty.EASY -> BotProfile.easy()
            Difficulty.MEDIUM -> BotProfile.medium()
            Difficulty.HARD -> BotProfile.hard()
        }
        seats = mutableListOf()
        bots = mutableMapOf()
        seats.add(Player("you", playerName.ifBlank { "You" }, config.startingStack, false))
        for (i in 0 until botCount) {
            val p = Player("bot$i", botName(i), config.startingStack, true)
            seats.add(p)
            bots[p.id] = PokerBot(profiles[i % profiles.size])
        }
        buttonIndex = seats.indices.random()
        history.clear()
        startHand()
    }

    fun startHand() {
        autoAdvanceJob?.cancel() // supersede any pending auto-deal
        val playable = seats.count { it.chips > 0 }
        if (playable < 2) {
            publish(message = if (seats[0].chips <= 0) "You're out of chips. Rebuy to play again." else "You win — everyone else is broke!")
            return
        }
        // Advance the button to the next player who still has chips.
        buttonIndex = nextWithChips(buttonIndex)
        winners = emptySet()
        lastActions.clear()
        val e = HandEngine(config, seats, buttonIndex, Deck())
        engine = e
        e.startHand()
        publishEngine("Hand dealt")
        runLoop()
    }

    /** Human seat actions, invoked from the UI. */
    fun humanAction(action: Action) {
        val e = engine ?: return
        if (e.currentActor()?.isBot != false) return
        try {
            e.act(action)
        } catch (ex: IllegalArgumentException) {
            return // ignore illegal input; UI only offers legal actions
        }
        lastActions[seats[0].id] = describe(action)
        publishEngine(null)
        runLoop()
    }

    fun rebuy() {
        if (seats.isNotEmpty() && seats[0].chips <= 0) {
            seats[0].chips = config.startingStack
            publish(message = "Rebought ${config.startingStack} chips")
        }
    }

    fun sendEmote(emote: String) {
        val cur = _state.value
        val seatsUi = cur.seats.map { if (it.isHuman) it.copy(emote = emote) else it }
        _state.value = cur.copy(seats = seatsUi)
    }

    // ------------------------------------------------------------- turn loop

    private fun runLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            val e = engine ?: return@launch
            while (isActive && !e.isComplete()) {
                val actor = e.currentActor() ?: break
                if (actor.isBot) {
                    publishEngine(null)
                    delay(700 + (Math.random() * 700).toLong()) // "thinking"
                    val bot = bots[actor.id]!!
                    val decision = bot.decide(e, actor)
                    e.act(decision)
                    lastActions[actor.id] = describe(decision)
                    publishEngine(null)
                    delay(250)
                } else {
                    // Human's turn: hand control to the UI and run the countdown.
                    publishEngine("Your turn")
                    runTurnTimer(e, actor)
                    return@launch
                }
            }
            if (e.isComplete()) finishHand(e)
        }
    }

    private suspend fun runTurnTimer(e: HandEngine, actor: Player) {
        val start = System.currentTimeMillis()
        while (isActive) {
            val elapsed = System.currentTimeMillis() - start
            val frac = (1f - elapsed.toFloat() / turnTimerMs).coerceIn(0f, 1f)
            _state.value = _state.value.copy(turnFraction = frac)
            if (e.currentActor() !== actor) return // human already acted (state advanced elsewhere)
            if (elapsed >= turnTimerMs) {
                // Auto-act: check if free, otherwise fold.
                val legal = e.legalActions(actor)
                val auto = if (legal.contains(Action.Type.CHECK)) Action.check() else Action.fold()
                humanAction(auto)
                return
            }
            delay(100)
        }
    }

    private fun finishHand(e: HandEngine) {
        val res = e.result()
        winners = res?.winnings?.keys?.map { it.id }?.toSet() ?: emptySet()
        val msg = buildString {
            res?.awards?.forEach { a ->
                val who = a.winners.joinToString(", ") { it.name }
                val hand = a.winningHand?.describe()?.let { " with $it" } ?: ""
                append("$who wins ${a.potAmount}$hand. ")
            }
        }.trim().ifBlank { "Hand complete" }
        // Record hand history (most recent first, capped at 12).
        history.addFirst(msg)
        while (history.size > 12) history.removeLast()
        publishEngine(msg, showdown = res?.wonByFold == false)
        // Auto-deal the next hand after a pause (cancellable if the player deals manually).
        autoAdvanceJob = viewModelScope.launch {
            delay(if (res?.wonByFold == false) 3200 else 1600)
            startHand()
        }
    }

    // ------------------------------------------------------------- projection

    private fun publishEngine(message: String?, showdown: Boolean = false) {
        val e = engine
        if (e == null) { publish(message = message ?: ""); return }
        val human = seats[0]
        val actor = e.currentActor()
        val humansTurn = actor != null && !actor.isBot
        val legal = if (humansTurn) e.legalActions(human).toSet() else emptySet()

        val seatsUi = seats.mapIndexed { idx, p ->
            SeatUi(
                name = p.name,
                chips = p.chips,
                streetBet = p.streetBet,
                hole = p.hole.toList(),
                revealHole = !p.isBot || (showdown && p.inHand && !p.folded && e.isComplete()),
                folded = p.folded,
                allIn = p.allIn,
                isHuman = !p.isBot,
                isButton = idx == buttonIndex,
                isDealerBlind = blindLabel(idx, e),
                isActing = actor === p,
                isWinner = winners.contains(p.id),
                lastAction = lastActions[p.id],
                emote = null,
            )
        }
        _state.value = TableState(
            seats = seatsUi,
            board = e.board.toList(),
            pot = e.potTotal(),
            phaseLabel = e.phase.name.lowercase().replaceFirstChar { it.uppercase() },
            message = message ?: _state.value.message,
            handInProgress = !e.isComplete(),
            humansTurn = humansTurn,
            legal = legal,
            toCall = if (humansTurn) e.amountToCall(human) else 0,
            minRaiseTo = if (humansTurn) e.minRaiseTo() else 0,
            maxRaiseTo = if (humansTurn) e.maxRaiseTo(human) else 0,
            bigBlind = config.bigBlind,
            turnFraction = 1f,
            handHistory = history.toList(),
            showdownActive = showdown && e.isComplete(),
        )
    }

    private fun publish(message: String) {
        _state.value = _state.value.copy(message = message, humansTurn = false, handInProgress = false)
    }

    private fun blindLabel(idx: Int, e: HandEngine): String? {
        // Recompute SB/BB purely for display.
        val inHand = seats.count { it.inHand }
        val sb: Int; val bb: Int
        if (inHand == 2) { sb = buttonIndex; bb = nextInHand(buttonIndex) }
        else { sb = nextInHand(buttonIndex); bb = nextInHand(sb) }
        return when (idx) { sb -> "SB"; bb -> "BB"; else -> null }
    }

    private fun nextInHand(from: Int): Int {
        val n = seats.size
        for (step in 1..n) { val i = (from + step) % n; if (seats[i].inHand) return i }
        return from
    }

    private fun nextWithChips(from: Int): Int {
        val n = seats.size
        for (step in 1..n) { val i = (from + step) % n; if (seats[i].chips > 0) return i }
        return from
    }

    private fun describe(a: Action): String = when (a.type) {
        Action.Type.FOLD -> "Fold"
        Action.Type.CHECK -> "Check"
        Action.Type.CALL -> "Call"
        Action.Type.BET -> "Bet ${a.amount}"
        Action.Type.RAISE -> "Raise ${a.amount}"
        Action.Type.ALL_IN -> "All-in"
    }

    private fun botName(i: Int): String {
        val names = listOf("Ava", "Marcus", "Priya", "Diego", "Nina", "Kwan", "Sofia", "Leo")
        return names[i % names.size]
    }

    override fun onCleared() {
        loopJob?.cancel()
        super.onCleared()
    }
}
