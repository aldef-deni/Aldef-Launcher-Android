package com.aldef.launcher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Deteksi geser-ke-atas di seluruh area, diproses pada [PointerEventPass.Initial]
 * sehingga induk melihat gerakan lebih dulu daripada anak yang bisa diklik.
 *
 * Tanpa ini, menggeser tepat di atas arc reactor akan dianggap ketukan dan
 * malah menyalakan mikrofon. Setelah ambang terlampaui, event dikonsumsi agar
 * ketukan pada anak dibatalkan.
 */
fun Modifier.swipeUpToOpen(
    thresholdPx: Float = 140f,
    onTriggered: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            var event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.pressed } ?: continue
            val startY = down.position.y
            var fired = false

            while (true) {
                event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break

                if (!fired && change.position.y - startY < -thresholdPx) {
                    fired = true
                    onTriggered()
                }
                // Konsumsi sisa gerakan supaya anak tidak menganggapnya ketukan.
                if (fired) change.consume()
            }
        }
    }
}

/**
 * Ketukan murni: hanya terpicu kalau jari diangkat tanpa bergeser melewati
 * touch slop. Dipakai untuk arc reactor supaya gerakan geser-ke-atas di
 * atasnya tidak salah dibaca sebagai perintah "mulai mendengarkan".
 *
 * [Modifier.clickable] tidak cukup di sini karena ia tetap memicu klik selama
 * tidak ada node lain yang mengonsumsi gerakannya.
 */
fun Modifier.tapOnly(onTap: () -> Unit): Modifier = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown()
        var moved = false

        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull() ?: break
            if (!change.pressed) break
            if ((change.position - down.position).getDistance() > slop) moved = true
        }

        if (!moved) onTap()
    }
}
