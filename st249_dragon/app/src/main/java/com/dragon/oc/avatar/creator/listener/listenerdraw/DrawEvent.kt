package com.dragon.oc.avatar.creator.listener.listenerdraw

import android.view.MotionEvent
import com.dragon.oc.avatar.creator.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}