@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.homepoker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homepoker.game.Difficulty
import com.homepoker.ui.theme.*

/** Three-button home screen: Play with Friends, Quick Play (bots), Tournaments. */
@Composable
fun HomeScreen(onQuickPlay: (Difficulty, Int) -> Unit) {
    var showDifficulty by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FeltGreenLight, FeltGreen))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 460.dp).padding(24.dp),
        ) {
            Text("♠ HOME POKER ♥", color = Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("Texas Hold'em — a home game in your pocket", color = Color(0xFFD8D0BE), fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))

            HomeButton("🎉  Play with Friends") { showFriends = true }
            Spacer(Modifier.height(12.dp))
            HomeButton("🤖  Quick Play (Bots)") { showDifficulty = true }
            Spacer(Modifier.height(12.dp))
            HomeButton("🏆  Tournaments (Sit & Go)") { onQuickPlay(Difficulty.HARD, 8) }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { showTutorial = true }) {
                Text("How to play (2-min tutorial)", color = Gold)
            }
        }
    }

    if (showDifficulty) {
        DifficultyDialog(
            onPick = { diff -> showDifficulty = false; onQuickPlay(diff, 5) },
            onDismiss = { showDifficulty = false },
        )
    }
    if (showFriends) {
        AlertDialog(
            onDismissRequest = { showFriends = false },
            confirmButton = { TextButton(onClick = { showFriends = false }) { Text("Got it") } },
            title = { Text("Private tables") },
            text = {
                Text(
                    "Online tables with shareable room codes, emotes, reconnect handling and a " +
                        "server-authoritative shuffle are the next milestone (see README → Roadmap). " +
                        "The full rules engine, side-pots and AI shipped here are the same ones the " +
                        "online tables will run — so for now, Quick Play vs. bots is fully playable offline."
                )
            },
        )
    }
    if (showTutorial) TutorialDialog { showTutorial = false }
}

@Composable
private fun HomeButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1206)),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DifficultyDialog(onPick: (Difficulty) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Choose difficulty") },
        text = {
            Column {
                DiffRow("Easy", "Loose, passive opponents — great for learning") { onPick(Difficulty.EASY) }
                DiffRow("Medium", "A balanced mix of styles, some bluffing") { onPick(Difficulty.MEDIUM) }
                DiffRow("Hard", "Tight-aggressive sharks that punish mistakes") { onPick(Difficulty.HARD) }
            }
        },
    )
}

@Composable
private fun DiffRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF5B5B5B))
        }
    }
}

@Composable
private fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Let's play") } },
        title = { Text("Texas Hold'em in 2 minutes") },
        text = {
            Column {
                Text("• You get 2 private cards. Five shared cards come out over 4 rounds " +
                    "(pre-flop, flop, turn, river).", fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("• Make your best 5-card hand from your 2 + the 5 shared cards.", fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("• Each round you can Check/Call, Bet/Raise, or Fold. Last player " +
                    "standing — or the best hand at showdown — wins the pot.", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Hand ranking (strong → weak):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Royal Flush › Straight Flush › Four of a Kind › Full House › Flush › " +
                    "Straight › Three of a Kind › Two Pair › Pair › High Card", fontSize = 12.sp)
            }
        },
    )
}
