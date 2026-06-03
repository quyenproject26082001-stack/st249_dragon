package com.female.maker.oc.creator2.listener.listenerdraw

import android.view.MotionEvent
import com.female.maker.oc.creator2.core.custom.drawview.DrawView

class DeleteEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (!tattooView!!.isLocking()) {
            tattooView.removeDrawCurrent()
        }
    }
}
