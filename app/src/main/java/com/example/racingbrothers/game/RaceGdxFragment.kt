package com.example.racingbrothers.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.example.racingbrothers.audio.ArcadeSoundManager
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.storage.GraphicsQuality

/**
 * Official libGDX AndroidFragmentApplication host embedded inside RaceActivity.
 * Allows Jetpack Compose HUD overlays to layer cleanly above the OpenGL ES 3D surface.
 */
class RaceGdxFragment : AndroidFragmentApplication() {

    var gameInstance: RacingBrothersGdxGame? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val host = activity as? RaceGdxHost
            ?: error("RaceGdxFragment must be attached to a RaceGdxHost activity.")

        val quality = host.getGraphicsQuality()
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useWakelock = true
            numSamples = if (quality == GraphicsQuality.HIGH) 2 else 0
            r = 8
            g = 8
            b = 8
            a = 0
            depth = 16
        }

        val game = RacingBrothersGdxGame(
            sessionManager = MultiplayerSessionManager.instance,
            initialCameraMode = host.getInitialCameraMode(),
            graphicsQuality = quality,
            soundManager = host.getSoundManager(),
            inputProvider = { host.readCurrentInput() },
            onHudUpdated = { snapshot -> host.onHudSnapshot(snapshot) }
        )
        gameInstance = game
        return initializeForView(game, config)
    }

    interface RaceGdxHost {
        fun getGraphicsQuality(): GraphicsQuality
        fun getInitialCameraMode(): CameraMode
        fun getSoundManager(): ArcadeSoundManager
        fun readCurrentInput(): RacingInputState
        fun onHudSnapshot(snapshot: RaceHudSnapshot)
    }
}
