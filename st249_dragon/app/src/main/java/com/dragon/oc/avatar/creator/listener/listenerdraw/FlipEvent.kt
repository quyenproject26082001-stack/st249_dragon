package com.dragon.oc.avatar.creator.listener.listenerdraw

import android.view.MotionEvent
import com.dragon.oc.avatar.creator.core.custom.drawview.DrawView
import com.dragon.oc.avatar.creator.core.utils.key.DrawKey


class FlipEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (tattooView != null && tattooView.getStickerCount() > 0) tattooView.flipCurrentDraw(
            DrawKey.FLIP_HORIZONTALLY)
    }
}