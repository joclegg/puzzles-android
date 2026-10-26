package com.joclegg.puzzles.sudoku.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object SudokuIcons {
    val Pencil = outline("pencil", "M4 20l1-4L15.5 5.5a2.1 2.1 0 0 1 3 3L8 19z", "M13.5 7.5l3 3")
    val Eraser =
        outline(
            "eraser",
            "M8.5 19L3.8 14.3a1.5 1.5 0 0 1 0-2.1L12.2 3.8a1.5 1.5 0 0 1 2.1 0L20.2 9.7a1.5 1.5 0 0 1 0 2.1L12.9 19z",
            "M7.5 8.5l8 8",
            "M12.9 19H20",
        )
    val Undo = outline("undo", "M9 14L4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
    val Mark = outline("mark", "M5 5h14v14H5z")
    val Redo = outline("redo", "M15 14l5-5-5-5", "M20 9H9.5a5.5 5.5 0 0 0 0 11H13")
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
    val Forward = outline("forward", "M9 5l7 7-7 7")
    val Calendar = outline("calendar", "M4 6h16v14H4z", "M4 10h16", "M8 3v4", "M16 3v4")
    val Clear = outline("clear", "M6 6l12 12", "M18 6L6 18")
    val Check = outline("check", "M5 12.5l4.5 4.5L19 7.5")
    val Cage =
        outline(
            "cage",
            "M4 4h3",
            "M10 4h4",
            "M17 4h3v3",
            "M20 10v4",
            "M20 17v3h-3",
            "M14 20h-4",
            "M7 20H4v-3",
            "M4 14v-4",
            "M4 7V4",
            "M7.5 9.5V7.5h2",
        )

    val CellFirst = outline("cell_first", "M4 4h16v16H4z", "M9.33 4v16", "M14.67 4v16", "M4 9.33h16", "M4 14.67h16")

    val NumberFirst =
        outline(
            "number_first",
            "M10 9.6l2.4-1.6v8",
            "M10 16h4.8",
            "M7.5 2v5.5H2",
            "M16.5 2v5.5H22",
            "M7.5 22v-5.5H2",
            "M16.5 22v-5.5H22",
        )

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
