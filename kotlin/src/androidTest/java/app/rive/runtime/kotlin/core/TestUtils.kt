package app.rive.runtime.kotlin.core

import android.content.Context
import android.graphics.RectF
import androidx.test.platform.app.InstrumentationRegistry
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.RiveArtboardRenderer
import app.rive.runtime.kotlin.controllers.RiveFileController
import org.junit.Assert.assertEquals
import java.util.concurrent.TimeoutException
import kotlin.time.Duration


class TestUtils {

    val context: Context by lazy {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("app.rive.runtime.kotlin.test", appContext.packageName)

        Rive.init(appContext)

        appContext
    }

    companion object {
        fun waitUntil(
            atMost: Duration,
            condition: () -> Boolean
        ) {
            val maxTime = atMost.inWholeMilliseconds

            val interval: Long = 50
            var elapsed: Long = 0
            do {
                elapsed += interval
                Thread.sleep(interval)

                if (elapsed > maxTime) {
                    throw TimeoutException("Took too long.")
                }
            } while (!condition())

        }
    }


    class MockArtboardRenderer : RiveArtboardRenderer() {
        /**
         * Instead of scheduling a new frame via the Choreographer (which uses a native C++ thread)
         * force an advance cycle. (We don't need to draw in tests either).
         */
        init {
//            lets just pretend we have a surface..
            isAttached = true
        }

        override fun scheduleFrame() {
            advance(0f)
        }

        /** NOP */
        override fun draw() {}
    }

    /**
     * This RiveAnimationView uses a custom [MockArtboardRenderer] to prevent tests from using the
     * Choreographer API which would be calling native threading primitives.
     */
    class MockRiveAnimationView(context: Context, attachOnInit: Boolean = true) :
        RiveAnimationView(context) {

        init {
            // Simulate this lifecycle method which the test harness wouldn't trigger otherwise.
            if (attachOnInit) {
                mockAttach()
            }
        }

        override fun createRenderer(): MockArtboardRenderer {
            return MockArtboardRenderer()
        }

        fun mockAttach() {
            onAttachedToWindow()
        }

        fun mockDetach() {
            onDetachedFromWindow()
        }
    }

    class MockNoopArtboardRenderer : RiveArtboardRenderer() {
        /** NOP */
        override fun scheduleFrame() {}

        /** NOP */
        override fun draw() {}
    }

    /**
     * This RiveAnimationView uses a custom [MockNoopArtboardRenderer] to noop any drawing interactions.
     */
    class MockNoopRiveAnimationView(context: Context) : RiveAnimationView(context) {
        init {
            // Simulate this lifecycle method which the test harness wouldn't trigger otherwise.
            onAttachedToWindow()
        }

        override fun createRenderer(): MockNoopArtboardRenderer {
            return MockNoopArtboardRenderer()
        }


        fun setBounds(width: Float, height: Float) {
            (renderer as MockNoopArtboardRenderer).targetBounds = RectF(0f, 0f, width, height)
        }
    }

    data class StateChanged(var stateMachineName: String, var stateName: String)

    class Observer : RiveFileController.Listener {
        var plays = mutableListOf<PlayableInstance>()
        var pauses = mutableListOf<PlayableInstance>()
        var stops = mutableListOf<PlayableInstance>()
        var loops = mutableListOf<PlayableInstance>()
        var states = mutableListOf<StateChanged>()

        override fun notifyPlay(animation: PlayableInstance) {
            plays.add(animation)
        }

        override fun notifyPause(animation: PlayableInstance) {
            pauses.add(animation)
        }

        override fun notifyStop(animation: PlayableInstance) {
            stops.add(animation)
        }

        override fun notifyLoop(animation: PlayableInstance) {
            loops.add(animation)
        }

        override fun notifyStateChanged(stateMachineName: String, stateName: String) {
            states.add(StateChanged(stateMachineName, stateName))
        }
    }
}
