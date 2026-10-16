package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.AircraftModel
import com.example.data.TrailEffect
import com.example.data.TriggerDirection
import com.example.data.VaultPreferences
import com.example.data.VaultRepository
import com.example.game.AirplaneGameCanvas
import com.example.game.DeceptivePlayerModal
import com.example.sound.SoundManager
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.vault.HangarSettingsScreen
import com.example.ui.vault.VaultHomeScreen

enum class ScreenState {
    GAME,
    DECEPTIVE_MODAL,
    SECRET_VAULT,
    DECOY_VAULT,
    HANGAR
}

class MainActivity : ComponentActivity() {

    private lateinit var prefs: VaultPreferences
    private lateinit var repository: VaultRepository
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = VaultPreferences(applicationContext)
        repository = VaultRepository(applicationContext)
        soundManager = SoundManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF070B19)
                ) {
                    SkyPilotVaultApp(
                        prefs = prefs,
                        repository = repository,
                        soundManager = soundManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun SkyPilotVaultApp(
    prefs: VaultPreferences,
    repository: VaultRepository,
    soundManager: SoundManager
) {
    var currentScreen by remember { mutableStateOf(ScreenState.GAME) }

    soundManager.isSoundEnabled = prefs.isSoundEnabled

    // Manage Engine Hum sound loop based on current screen
    DisposableEffect(currentScreen) {
        if (currentScreen == ScreenState.GAME || currentScreen == ScreenState.DECEPTIVE_MODAL) {
            soundManager.startEngineHum()
        } else {
            soundManager.stopEngineHum()
        }
        onDispose {
            soundManager.stopEngineHum()
        }
    }

    BackHandler(enabled = currentScreen != ScreenState.GAME) {
        when (currentScreen) {
            ScreenState.DECEPTIVE_MODAL -> currentScreen = ScreenState.GAME
            ScreenState.SECRET_VAULT -> currentScreen = ScreenState.GAME
            ScreenState.DECOY_VAULT -> currentScreen = ScreenState.GAME
            ScreenState.HANGAR -> currentScreen = ScreenState.GAME
            ScreenState.GAME -> { /* System default exit */ }
        }
    }

    val currentAircraft = remember(prefs.selectedAircraftId, currentScreen) {
        AircraftModel.values().find { it.id == prefs.selectedAircraftId } ?: AircraftModel.SU_57_ALGERIAN
    }

    val currentTrail = remember(prefs.selectedTrailId, currentScreen) {
        TrailEffect.values().find { it.id == prefs.selectedTrailId } ?: TrailEffect.PLASMA_BLUE
    }

    val currentTrigger = remember(prefs.triggerDirectionId, currentScreen) {
        TriggerDirection.values().find { it.id == prefs.triggerDirectionId } ?: TriggerDirection.ANY_SIDE
    }

    when (currentScreen) {
        ScreenState.GAME, ScreenState.DECEPTIVE_MODAL -> {
            AirplaneGameCanvas(
                aircraftModel = currentAircraft,
                trailEffect = currentTrail,
                triggerDirection = currentTrigger,
                soundManager = soundManager,
                onTriggerDeceptiveModal = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.DECEPTIVE_MODAL
                },
                onOpenHangar = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.HANGAR
                }
            )

            if (currentScreen == ScreenState.DECEPTIVE_MODAL) {
                DeceptivePlayerModal(
                    prefs = prefs,
                    soundManager = soundManager,
                    onDismiss = {
                        soundManager.playUiClickSound()
                        currentScreen = ScreenState.GAME
                    },
                    onUnlockRealVault = {
                        soundManager.playUiClickSound()
                        currentScreen = ScreenState.SECRET_VAULT
                    },
                    onUnlockDecoyVault = {
                        soundManager.playUiClickSound()
                        currentScreen = ScreenState.DECOY_VAULT
                    }
                )
            }
        }

        ScreenState.SECRET_VAULT -> {
            VaultHomeScreen(
                repository = repository,
                isDecoyVault = false,
                onLockAndReturnToGame = {
                    repository.clearTempCache()
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.GAME
                },
                onOpenHangar = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.HANGAR
                }
            )
        }

        ScreenState.DECOY_VAULT -> {
            VaultHomeScreen(
                repository = repository,
                isDecoyVault = true,
                onLockAndReturnToGame = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.GAME
                },
                onOpenHangar = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.HANGAR
                }
            )
        }

        ScreenState.HANGAR -> {
            HangarSettingsScreen(
                prefs = prefs,
                onBack = {
                    soundManager.playUiClickSound()
                    currentScreen = ScreenState.GAME
                }
            )
        }
    }
}
