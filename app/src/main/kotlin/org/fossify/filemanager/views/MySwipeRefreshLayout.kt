package org.fossify.filemanager.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

class MySwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // Setting "isEnabled = false" is recommended for users of this ViewGroup
        // who who are not interested in the pull to refresh functionality
        // Setting this easily avoids executing code needlessly before the check for "canChildScrollUp".
        if (!isEnabled) {
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        // Ignoring nested scrolls from descendants.
        // Allowing descendants to trigger nested scrolls would defeat the purpose of this class
        // and result in pull to refresh to happen for all movements on the Y axis
        // (even as part of scale/quick scale gestures) while also doubling the throbber with the overscroll shadow.
        return if (isEnabled) {
            return false
        } else {
            super.onStartNestedScroll(child, target, nestedScrollAxes)
        }
    }

    override fun canChildScrollUp(): Boolean {
        val directChild = getChildAt(0)
        return when (directChild) {
            is RecyclerViewFastScroller -> {
                val innerRecyclerView = directChild.getChildAt(0)
                innerRecyclerView?.canScrollVertically(-1) == true
            }
            else -> super.canChildScrollUp()
        }
    }
}
