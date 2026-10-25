package com.joclegg.puzzles.slideout.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SlideOutGameIcon(modifier: Modifier = Modifier) {
    val colors = LocalSlideColors.current
    Box(modifier.size(56.dp).background(colors.board, RoundedCornerShape(8.dp))) {
        Box(Modifier.offset(x = 4.dp, y = 22.dp).size(30.dp, 13.dp).background(colors.target, RoundedCornerShape(3.dp)))
        Box(Modifier.offset(x = 37.dp, y = 4.dp).size(14.dp, 30.dp).background(colors.block, RoundedCornerShape(3.dp)))
        Box(Modifier.offset(x = 4.dp, y = 39.dp).size(30.dp, 13.dp).background(colors.block, RoundedCornerShape(3.dp)))
        Box(Modifier.offset(x = 53.dp, y = 22.dp).size(3.dp, 13.dp).background(colors.exit))
    }
}
