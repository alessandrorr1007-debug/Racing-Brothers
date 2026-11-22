package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.example.racingbrothers.ui.screens.CarSelectionScreen
import com.example.racingbrothers.ui.screens.CharacterSelectionScreen
import com.example.ui.theme.RacingBrothersTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w568dp-h300dp-land-hdpi")
class CompactSelectionRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `vehicle and driver controls are reachable with large text in compact landscape`() {
        var confirmedCar: Pair<String, String>? = null
        var confirmedDriver: String? = null
        compose.setContent {
            var vehicleId by remember { mutableStateOf<String?>(null) }
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.8f)
            ) {
                RacingBrothersTheme {
                    val chosenVehicle = vehicleId
                    if (chosenVehicle == null) {
                        CarSelectionScreen(
                            initialVehicleId = "vortex_rs",
                            initialColorId = "electric_cobalt",
                            roomCode = "004821",
                            onBack = {},
                            onConfirmCar = { vehicle, color ->
                                confirmedCar = vehicle to color
                                vehicleId = vehicle
                            }
                        )
                    } else {
                        CharacterSelectionScreen(
                            initialCharacterId = "nico_turbo",
                            pairedVehicleId = chosenVehicle,
                            roomCode = "004821",
                            onBack = { vehicleId = null },
                            onConfirmCharacter = { confirmedDriver = it }
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("btn_car_back").assertIsDisplayed()
        compose.onNodeWithTag("btn_confirm_car").assertIsDisplayed()
        compose.onNodeWithTag("btn_next_car").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("car_model_name").performScrollTo()
            .assertTextContains("APEX GT-X", substring = true)
        compose.onNodeWithTag("color_swatch_carbon_stealth").performScrollTo()
            .assertIsDisplayed().performClick()
        compose.onNodeWithTag("btn_confirm_car").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("apex_gtx" to "carbon_stealth", confirmedCar) }

        compose.onNodeWithTag("btn_char_back").assertIsDisplayed()
        compose.onNodeWithTag("btn_confirm_character").assertIsDisplayed()
        compose.onNodeWithTag("btn_next_character").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("character_name").performScrollTo().assertTextContains("KIRA", substring = true)
        compose.onNodeWithTag("char_card_beto_torque").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("btn_confirm_character").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("beto_torque", confirmedDriver) }
    }
}
