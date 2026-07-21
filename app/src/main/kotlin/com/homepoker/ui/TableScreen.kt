@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.homepoker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homepoker.engine.Action
import com.homepoker.game.SeatUi
import com.homepoker.game.TableState
import com.homepoker.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TableScreen(
    state: TableState,
    onAction: (Action) -> Unit,
    onEmote: (String) -> Unit,
    onNewHand: () -> Unit,
    onRebuy: () -> Unit,
    onExit: () -> Unit,
) {
    var showHistory by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(FeltGreenLight, FeltGreen), radius = 1400f))
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {

            // Top bar: pot / phase / menu
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onExit) { Text("‹ Lobby", color = Gold) }
                Text(
                    "${state.phaseLabel}  •  Pot ${state.pot}",
                    color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                )
                TextButton(onClick = { showHistory = true }) { Text("History", color = Gold) }
            }

            // Opponents
            val opponents = state.seats.filterNot { it.isHuman }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                opponents.forEach { SeatView(it, revealHidden = false) }
            }

            // Center: board + pot
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until 5) {
                        val c = state.board.getOrNull(i)
                        if (c != null) CardView(c, faceUp = true, width = 52.dp)
                        else EmptyCardSlot()
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.pot > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x66000000))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) { Text("Pot: ${state.pot}", color = Gold, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(6.dp))
                Text(state.message, color = Color(0xFFEDE6D6), fontSize = 13.sp)
            }

            // Human seat
            val human = state.seats.firstOrNull { it.isHuman }
            if (human != null) SeatView(human, revealHidden = true, large = true)

            // Action / control bar
            ControlBar(state, onAction, onEmote, onNewHand, onRebuy)
        }
    }

    if (showHistory) HandHistoryDialog(state.handHistory) { showHistory = false }
}

@Composable
private fun EmptyCardSlot() {
    Box(
        Modifier
            .size(52.dp, 74.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0x22000000))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(7.dp))
    )
}

@Composable
private fun SeatView(seat: SeatUi, revealHidden: Boolean, large: Boolean = false) {
    val ringColor = when {
        seat.isWinner -> Gold
        seat.isActing -> Color(0xFF7CFF9E)
        else -> Color(0x33FFFFFF)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Bet chips in front of the seat
        if (seat.streetBet > 0) {
            Text("● ${seat.streetBet}", color = Gold, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val cardW = if (large) 58.dp else 34.dp
            if (seat.hole.isEmpty() && seat.folded) {
                // folded, no cards
            } else {
                val show = seat.revealHole || (revealHidden && seat.isHuman)
                seat.hole.forEachIndexed { i, c ->
                    CardView(c, faceUp = show, width = cardW)
                }
                if (seat.hole.isEmpty()) {
                    CardView(null, faceUp = false, width = cardW)
                    CardView(null, faceUp = false, width = cardW)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (seat.folded) Color(0x55000000) else Color(0x99000000))
                .border(2.dp, ringColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (seat.isButton) DealerChip()
                    Text(
                        seat.name + (seat.isDealerBlind?.let { " ($it)" } ?: ""),
                        color = if (seat.folded) Color(0xFF9E9E9E) else Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    if (seat.allIn) "ALL-IN" else "${seat.chips}",
                    color = if (seat.allIn) ChipRed else Gold, fontSize = 12.sp,
                )
            }
        }
        val label = seat.emote ?: seat.lastAction
        if (label != null) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE6FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text(label, color = Color(0xFF14532D), fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun DealerChip() {
    Box(
        Modifier.padding(end = 4.dp).size(16.dp).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) { Text("D", color = Color(0xFF14532D), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ControlBar(
    state: TableState,
    onAction: (Action) -> Unit,
    onEmote: (String) -> Unit,
    onNewHand: () -> Unit,
    onRebuy: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        // Emote row (always available while a hand is in progress)
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            listOf("👍 Nice hand", "😅", "🔥 All in!", "🤔", "😂").forEach { emo ->
                AssistChip(onClick = { onEmote(emo) }, label = { Text(emo, fontSize = 12.sp) })
            }
        }

        if (state.humansTurn) {
            // Turn timer
            val frac by animateFloatAsState(state.turnFraction, label = "turn")
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (frac < 0.3f) ChipRed else Gold,
            )
            Spacer(Modifier.height(6.dp))
            ActionButtons(state, onAction)
        } else if (!state.handInProgress) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                val humanChips = state.seats.firstOrNull { it.isHuman }?.chips ?: 1
                if (humanChips <= 0) Button(onClick = onRebuy) { Text("Rebuy") }
                Button(onClick = onNewHand) { Text("New Hand") }
            }
        } else {
            Text(
                "Waiting for opponents…",
                color = Color(0xFFCFC7B5), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
}

@Composable
private fun ActionButtons(state: TableState, onAction: (Action) -> Unit) {
    var raiseTo by remember(state.minRaiseTo, state.maxRaiseTo) {
        mutableStateOf(state.minRaiseTo.coerceAtMost(state.maxRaiseTo).toFloat())
    }
    val canRaise = state.legal.contains(Action.Type.BET) || state.legal.contains(Action.Type.RAISE)
    val canCheck = state.legal.contains(Action.Type.CHECK)

    // Raise sizing slider
    if (canRaise && state.maxRaiseTo > state.minRaiseTo) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Raise to ${raiseTo.roundToInt()}", color = Gold, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            listOf("½ Pot" to 0.5, "Pot" to 1.0, "Max" to -1.0).forEach { (lbl, mult) ->
                TextButton(onClick = {
                    raiseTo = if (mult < 0) state.maxRaiseTo.toFloat()
                    else (state.toCall + (state.pot * mult)).toFloat()
                        .coerceIn(state.minRaiseTo.toFloat(), state.maxRaiseTo.toFloat())
                }) { Text(lbl, fontSize = 11.sp, color = Color.White) }
            }
        }
        Slider(
            value = raiseTo,
            onValueChange = { raiseTo = it },
            valueRange = state.minRaiseTo.toFloat()..state.maxRaiseTo.toFloat(),
        )
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onAction(Action.fold()) },
            colors = ButtonDefaults.buttonColors(containerColor = ChipRed),
            modifier = Modifier.weight(1f),
        ) { Text("Fold") }

        Button(
            onClick = { onAction(if (canCheck) Action.check() else Action.call()) },
            colors = ButtonDefaults.buttonColors(containerColor = ChipBlue),
            modifier = Modifier.weight(1f),
        ) { Text(if (canCheck) "Check" else "Call ${state.toCall}") }

        if (canRaise) {
            Button(
                onClick = {
                    val to = raiseTo.roundToInt()
                    val act = if (state.legal.contains(Action.Type.BET)) Action.bet(to) else Action.raiseTo(to)
                    onAction(act)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1206)),
                modifier = Modifier.weight(1f),
            ) { Text(if (state.legal.contains(Action.Type.BET)) "Bet" else "Raise") }
        }
        if (state.legal.contains(Action.Type.ALL_IN)) {
            Button(
                onClick = { onAction(Action.allIn()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1F1F)),
                modifier = Modifier.weight(1f),
            ) { Text("All-in") }
        }
    }
}

@Composable
private fun HandHistoryDialog(history: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Recent hands") },
        text = {
            if (history.isEmpty()) Text("No hands played yet.")
            else Column {
                history.forEach { Text("• $it", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        },
    )
}
