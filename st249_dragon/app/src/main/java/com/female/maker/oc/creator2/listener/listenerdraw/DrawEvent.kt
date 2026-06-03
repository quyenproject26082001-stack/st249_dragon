package com.female.maker.oc.creator2.listener.listenerdraw

import android.view.MotionEvent
import com.female.maker.oc.creator2.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}