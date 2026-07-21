package com.homepoker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homepoker.engine.Card
import com.homepoker.engine.Suit
import com.homepoker.ui.theme.CardCream
import com.homepoker.ui.theme.FeltGreenLight
import com.homepoker.ui.theme.Gold

/** A single playing card, face-up or face-down, drawn from vectors (no image assets). */
@Composable
fun CardView(card: Card?, faceUp: Boolean, width: Dp = 46.dp, modifier: Modifier = Modifier) {
    val height = width * 1.42f
    val corner = width * 0.14f
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center,
    ) {
        if (faceUp && card != null) {
            val red = card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS
            val ink = if (red) Color(0xFFC0293B) else Color(0xFF1A1A1A)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CardCream)
                    .border(1.dp, Color(0x33000000), RoundedCornerShape(corner)),
            ) {
                Column(Modifier.padding(start = width * 0.12f, top = width * 0.06f)) {
                    Text(
                        text = card.rank.code.toString(),
                        color = ink,
                        fontSize = (width.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = card.suit.glyph, color = ink, fontSize = (width.value * 0.34f).sp)
                }
                Text(
                    text = card.suit.glyph,
                    color = ink,
                    fontSize = (width.value * 0.7f).sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        } else {
            // Card back: a subtle gold-on-green diamond weave.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(FeltGreenLight, Color(0xFF0A2E16)))
                    )
                    .border(1.5.dp, Gold, RoundedCornerShape(corner)),
                contentAlignment = Alignment.Center,
            ) {
                Text("♠", color = Gold.copy(alpha = 0.7f), fontSize = (width.value * 0.5f).sp)
            }
        }
    }
}
