package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object TwoToneIcons {
    val Undo = outline("undo", "M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
    val Back = outline("back", "M15 5l-7 7 7 7")
    val More =
        outline(
            "more",
            "M12 4.8a1.2 1.2 0 1 0 0.01 0z",
            "M12 10.8a1.2 1.2 0 1 0 0.01 0z",
            "M12 16.8a1.2 1.2 0 1 0 0.01 0z",
        )
    val Settings =
        outline(
            "settings",
            "M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22" +
                "l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84a.47.47 0 0 0-.47.41l-.36 2.54" +
                "a7.3 7.3 0 0 0-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58" +
                "c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22" +
                "l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54" +
                "a7 7 0 0 0 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.48.48 0 0 0-.12-.61z",
            "M12 8.6a3.4 3.4 0 1 1 0 6.8a3.4 3.4 0 1 1 0-6.8z",
        )
    val Clear = outline("clear", "M6 6l12 12", "M18 6L6 18")
    val Hint = outline("hint", "M9 18h6", "M10 21h4", "M12 3a6 6 0 0 0-3.5 10.9V16h7v-2.1A6 6 0 0 0 12 3z")
    val Rules = outline("rules", "M12 21a9 9 0 1 0 0-18a9 9 0 1 0 0 18z", "M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6v.6", "M12 17h.01")

    private fun outline(
        name: String,
        vararg paths: String,
    ): ImageVector =
        ImageVector
            .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                for (path in paths) {
                    addPath(
                        pathData = addPathNodes(path),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }.build()
}
