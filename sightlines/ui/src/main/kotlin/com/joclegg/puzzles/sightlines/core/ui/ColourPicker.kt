package com.joclegg.puzzles.sightlines.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.sightlines.engine.Dot

private val PickerShape = RoundedCornerShape(10.dp)

@Composable
fun ColourPicker(
    colour: Dot,
    onColourClick: (Dot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTileColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf(Dot.BLUE, Dot.RED, Dot.EMPTY).forEach { option ->
            val picked = option == colour
            val (fill, name) =
                when (option) {
                    Dot.RED -> colors.red to "Red"
                    Dot.BLUE -> colors.blue to "Blue"
                    Dot.EMPTY -> colors.emptyTile to "Clear"
                }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(PickerShape)
                    .background(fill)
                    .then(
                        if (picked) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, PickerShape)
                        } else {
                            Modifier
                        },
                    ).clickable { onColourClick(option) }
                    .testTag("colour_${option.name.lowercase()}")
                    .semantics {
                        selected = picked
                        contentDescription = name
                    },
            ) {
                if (option == Dot.EMPTY) {
                    Icon(SightlinesIcons.Clear, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
