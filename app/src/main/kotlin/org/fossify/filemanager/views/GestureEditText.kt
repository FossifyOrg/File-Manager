package org.fossify.filemanager.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.interfaces.GestureView
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.filemanager.extensions.config

// inspired by
// https://github.com/alexvasilkov/GestureViews/blob/f0a4c266e31dcad23bd0d9013531bc1c501b9c9f/sample/src/main/java/com/alexvasilkov/gestures/sample/ex/custom/text/GestureTextView.java
class GestureEditText : AppCompatEditText, GestureView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val controller: GestureController = GestureController(this)
    private var origSize = 0f
    private var size = 0f

    init {
        controller.settings.setOverzoomFactor(1f).isPanEnabled = false
        controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateChanged(state: State) {
                applyState(state)
            }

            override fun onStateReset(oldState: State, newState: State) {
                applyState(newState)
            }
        })

        origSize = textSize
        setTextColor(context.getProperTextColor())
        setLinkTextColor(context.getProperPrimaryColor())

        val storedTextZoom = context.config.editorTextZoom
        if (storedTextZoom != 0f) {
            onGlobalLayout {
                controller.state.zoomTo(storedTextZoom, width / 2f, height / 2f)
                controller.updateState()
            }
        }
    }

    override fun getController() = controller

    override fun onTouchEvent(event: MotionEvent): Boolean {
        controller.onTouch(this, event)
        return super.onTouchEvent(event)
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        origSize = textSize
        applyState(controller.state)
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        origSize = textSize
        applyState(controller.state)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        controller.settings.setViewport(width, height).setImage(width, height)
        controller.updateState()
    }

    private fun applyState(state: State) {
        var size = origSize * state.zoom
        val maxSize = origSize * controller.stateController.getMaxZoom(state)
        size = Math.max(origSize, Math.min(size, maxSize))

        size = Math.round(size).toFloat()
        if (!State.equals(this.size, size)) {
            this.size = size
            super.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            context.config.editorTextZoom = state.zoom
        }
    }
}
