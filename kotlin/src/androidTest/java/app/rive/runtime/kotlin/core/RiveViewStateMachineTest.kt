package app.rive.runtime.kotlin.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.test.R
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RiveViewStateMachineTest {

    @Test
    fun viewDefaultsLoadResouce() {
        UiThreadStatement.runOnUiThread {
            val appContext = initTests()

            val view = RiveAnimationView(appContext)
            view.setRiveResource(R.raw.multiple_state_machines, autoplay = false)
            view.play(listOf("one", "two"), areStateMachines = true)

            assertEquals(true, view.isPlaying)
            assertEquals(listOf("New Artboard",), view.file?.artboardNames)
            assertEquals(
                listOf("one", "two"),
                view.stateMachines.map { it.stateMachine.name }.toList()
            )
        }
    }

    @Test
    fun viewDefaultsNoAutoplay() {
        UiThreadStatement.runOnUiThread {
            val appContext = initTests()

            val view = RiveAnimationView(appContext)
            view.setRiveResource(R.raw.multiple_state_machines, autoplay = false)
            assertEquals(false, view.isPlaying)
            view.artboardName = "New Artboard"
            assertEquals(
                listOf<String>(),
                view.stateMachines.map { it.stateMachine.name }.toList()
            )
        }
    }

    @Test
    fun viewPause() {
        UiThreadStatement.runOnUiThread {
            val appContext = initTests()

            val view = RiveAnimationView(appContext)
            view.setRiveResource(R.raw.multiple_state_machines, stateMachineName = "one")
            assertEquals(true, view.isPlaying)
            assertEquals(0, view.animations.size)
            assertEquals(0, view.playingAnimations.size)
            assertEquals(1, view.stateMachines.size)
            assertEquals(1, view.playingStateMachines.size)
            view.pause()
            assertEquals(false, view.isPlaying)
            assertEquals(1, view.stateMachines.size)
            assertEquals(0, view.playingStateMachines.size)
        }
    }

}