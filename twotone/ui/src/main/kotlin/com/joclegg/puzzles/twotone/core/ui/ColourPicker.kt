package com.joclegg.puzzles.twotone.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joclegg.puzzles.twotone.engine.Tile

private val PickerShape = RoundedCornerShape(10.dp)

@Composable
fun ColourPicker(
    colour: Tile,
    onColourClick: (Tile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTileColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        listOf(
            Tile.RED to colors.red to "Red",
            Tile.BLUE to colors.blue to "Blue",
        ).forEach { (choice, name) ->
            val (option, fill) = choice
            val picked = option == colour
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
            )
        }
    }
}
