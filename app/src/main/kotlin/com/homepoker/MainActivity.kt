package com.homepoker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homepoker.game.Difficulty
import com.homepoker.game.GameViewModel
import com.homepoker.ui.HomeScreen
import com.homepoker.ui.TableScreen
import com.homepoker.ui.theme.HomePokerTheme

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePokerTheme {
                Surface(Modifier.fillMaxSize()) {
                    var atTable by remember { mutableStateOf(false) }
                    val state by vm.state.collectAsStateWithLifecycle()

                    if (!atTable) {
                        HomeScreen(onQuickPlay = { difficulty: Difficulty, bots: Int ->
                            vm.newGame("You", bots, difficulty)
                            atTable = true
                        })
                    } else {
                        TableScreen(
                            state = state,
                            onAction = vm::humanAction,
                            onEmote = vm::sendEmote,
                            onNewHand = vm::startHand,
                            onRebuy = vm::rebuy,
                            onExit = { atTable = false },
                        )
                    }
                }
            }
        }
    }
}
