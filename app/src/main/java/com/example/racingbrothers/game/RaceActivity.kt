package com.example.racingbrothers.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.example.racingbrothers.audio.ArcadeSoundManager
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.controls.RacingInputState
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.storage.PreferencesRepository
import com.example.racingbrothers.ui.screens.RaceHudOverlay
import com.example.ui.theme.RacingBrothersTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Landscape dedicated 3D Race Activity combining libGDX (RaceGdxFragment) and Jetpack Compose HUD.
 * Implements AndroidFragmentApplication.Callbacks for official libGDX lifecycle integration.
 */
class RaceActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks, RaceGdxFragment.RaceGdxHost {

    private val hudState = MutableStateFlow(RaceHudSnapshot())
    private val soundManager = ArcadeSoundManager()
    private lateinit var preferencesRepository: PreferencesRepository

    @Volatile
    private var steerLeft = false

    @Volatile
    private var steerRight = false

    @Volatile
    private var throttlePressed = false

    @Volatile
    private var brakePressed = false

    @Volatile
    private var nitroPressed = false

    private var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    private var initialCameraMode: CameraMode = CameraMode.CLOSE_CHASE
    private var gdxFragment: RaceGdxFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        preferencesRepository = PreferencesRepository(this)
        graphicsQuality = GraphicsQuality.fromId(intent.getStringExtra(EXTRA_GRAPHICS_QUALITY))
        initialCameraMode = CameraMode.fromId(
            intent.getStringExtra(EXTRA_CAMERA_MODE) ?: CameraMode.CLOSE_CHASE.id
        )
        soundManager.enabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val gdxContainerId = android.view.View.generateViewId()
        val gdxContainer = FrameLayout(this).apply {
            id = gdxContainerId
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(gdxContainer)

        val composeHud = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setContent {
                RacingBrothersTheme {
                    val sessionState by MultiplayerSessionManager.instance.state.collectAsStateWithLifecycle()
                    val hud by hudState.collectAsStateWithLifecycle()
                    RaceHudOverlay(
                        sessionState = sessionState,
                        hudSnapshot = hud,
                        onSteerLeftChanged = { steerLeft = it },
                        onSteerRightChanged = { steerRight = it },
                        onThrottleChanged = { throttlePressed = it },
                        onBrakeChanged = { brakePressed = it },
                        onNitroChanged = { nitroPressed = it },
                        onToggleCamera = {
                            gdxFragment?.gameInstance?.let { game ->
                                game.requestCameraToggle()
                                val nextMode = game.getCurrentCameraMode().next()
                                lifecycleScope.launch {
                                    preferencesRepository.saveCameraMode(nextMode)
                                }
                            }
                        },
                        onRequestRematch = {
                            soundManager.playUiClick()
                            MultiplayerSessionManager.instance.requestRematch()
                        },
                        onExitToMenu = {
                            soundManager.playUiClick()
                            MultiplayerSessionManager.instance.leaveRoom()
                            finish()
                        }
                    )
                }
            }
        }
        root.addView(composeHud)
        setContentView(root)

        if (savedInstanceState == null) {
            val fragment = RaceGdxFragment()
            gdxFragment = fragment
            supportFragmentManager.beginTransaction()
                .replace(gdxContainerId, fragment, TAG_GDX_FRAGMENT)
                .commit()
        } else {
            gdxFragment = supportFragmentManager.findFragmentByTag(TAG_GDX_FRAGMENT) as? RaceGdxFragment
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun getGraphicsQuality(): GraphicsQuality = graphicsQuality
    override fun getInitialCameraMode(): CameraMode = initialCameraMode
    override fun getSoundManager(): ArcadeSoundManager = soundManager

    override fun readCurrentInput(): RacingInputState {
        val steer = when {
            steerLeft && !steerRight -> -1f
            steerRight && !steerLeft -> 1f
            else -> 0f
        }
        return RacingInputState(
            throttle = if (throttlePressed) 1f else 0f,
            brake = if (brakePressed) 1f else 0f,
            steer = steer,
            nitro = nitroPressed
        )
    }

    override fun onHudSnapshot(snapshot: RaceHudSnapshot) {
        hudState.value = snapshot
    }

    override fun exit() {
        finish()
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }

    companion object {
        private const val TAG_GDX_FRAGMENT = "race_gdx_fragment"
        private const val EXTRA_GRAPHICS_QUALITY = "extra_graphics_quality"
        private const val EXTRA_CAMERA_MODE = "extra_camera_mode"
        private const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"

        fun createIntent(
            context: Context,
            graphicsQuality: GraphicsQuality,
            cameraMode: CameraMode,
            soundEnabled: Boolean
        ): Intent = Intent(context, RaceActivity::class.java).apply {
            putExtra(EXTRA_GRAPHICS_QUALITY, graphicsQuality.id)
            putExtra(EXTRA_CAMERA_MODE, cameraMode.id)
            putExtra(EXTRA_SOUND_ENABLED, soundEnabled)
        }
    }
}
