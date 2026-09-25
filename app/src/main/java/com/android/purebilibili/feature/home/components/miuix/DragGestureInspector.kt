package com.android.purebilibili.feature.home.components.miuix

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

suspend fun PointerInputScope.inspectMainPassDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent =
            drag(
                pointerId = down.id,
                onDrag = { onDrag(it, it.positionChange()) }
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    // [BB空间修复] 必须越过 touch slop 才认定为拖拽。
    //
    // 原实现没有任何阈值：手指按在指示器（顶栏标签 / 底栏选中项）那层
    // 透明命中区上，哪怕只抖动 1px 也会被 [onDrag] 累积成「选页位移」，
    // 抬手时 round 一下就可能落到相邻槽位，于是触发一次落页回调。
    // 表现就是用户反馈的「滑动时点到屏幕右边会自动跳到左边一页」。
    //
    // 这里改为先累积位移，超过 slop 才把累计量作为第一次拖拽交出；
    // slop 之内的抖动不再产生任何位移，抬手时 targetValue 不变、
    // round 结果仍等于当前选中项，自然不会误切页。
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)

        val down = awaitFirstDown(false)

        onDragStart(down)

        var pointer = initialDown.id
        var dragStarted = false
        var pendingDrag = Offset.Zero
        var upEvent: PointerInputChange? = null

        while (true) {
            val change = awaitDragOrUp(pointer) ?: break
            if (change.changedToUpIgnoreConsumed()) {
                upEvent = change
                break
            }
            pointer = change.id
            val delta = change.positionChange()
            if (dragStarted) {
                if (delta != Offset.Zero) {
                    change.consume()
                }
                onDrag(change, delta)
            } else {
                pendingDrag += delta
                if (pendingDrag.getDistance() > touchSlop) {
                    dragStarted = true
                    change.consume()
                    onDrag(change, pendingDrag)
                    pendingDrag = Offset.Zero
                }
            }
        }

        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
