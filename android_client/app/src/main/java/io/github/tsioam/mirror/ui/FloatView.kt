package io.github.tsioam.mirror.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FloatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var dX = 0f
    private var dY = 0f
    private var lastAction = 0
    private var relayoutOnMove = false
    private var topMargin = 0f
    public var moveListener: (() -> Unit)? = null

    init {
        id = View.generateViewId()
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            topMargin = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or  WindowInsetsCompat.Type.navigationBars()
            ).top.toFloat()
            requestLayout()
            insets
        }
        this.y = resources.displayMetrics.heightPixels * 0.2f
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dX = this.x - event.rawX
                dY = this.y - event.rawY
                lastAction = MotionEvent.ACTION_DOWN
            }
            MotionEvent.ACTION_MOVE -> {
                lastAction = MotionEvent.ACTION_MOVE
                this.x = fitX(event.rawX + dX)
                this.y = fitY(event.rawY + dY)
                if (relayoutOnMove) {
                    parent.requestLayout()
                }
                moveListener?.invoke()
                return false
            }
            MotionEvent.ACTION_UP -> {
                if (lastAction == MotionEvent.ACTION_MOVE) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val fitXValue = fitX(this.x)
        val fitYValue = fitY(this.y)
        if (fitXValue != x || fitYValue != y) {
            x = fitXValue
            y = fitYValue
            invalidate()
        }
    }

    private fun fitX(x: Float): Float {
        if (parent is View) {
            val maxX = ((parent as View).width - width).toFloat()
            return maxX.coerceAtMost(x).coerceAtLeast(0f)
        }
        return x.coerceAtLeast(0f)
    }

    private fun fitY(y: Float): Float {
        if (parent is View) {
            val maxY = ((parent as View).height - height).toFloat()
            return maxY.coerceAtMost(y).coerceAtLeast(topMargin)
        }
        return y.coerceAtLeast(topMargin)
    }

    fun needReLayoutOnMove() {
        relayoutOnMove = true
    }

    fun moveToBorder() {
        if (parent is View) {
            val parentWidth = (parent as View).width
            val isLeft = x + width/2 < parentWidth/2
            this.x = if (isLeft) 0f else (parentWidth - width).toFloat()
            if (relayoutOnMove) {
                parent.requestLayout()
            }
        }
    }

}