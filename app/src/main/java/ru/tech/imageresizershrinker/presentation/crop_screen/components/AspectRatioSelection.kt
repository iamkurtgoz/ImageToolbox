package ru.tech.imageresizershrinker.presentation.crop_screen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.CropAspectRatio
import com.smarttoolfactory.cropper.widget.AspectRatioSelectionCard
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.presentation.root.theme.outlineVariant
import ru.tech.imageresizershrinker.presentation.root.utils.modifier.container

@Composable
fun AspectRatioSelection(
    modifier: Modifier = Modifier,
    selectedIndex: Int = 2,
    onAspectRatioChange: (CropAspectRatio) -> Unit
) {
    val aspectRatios = aspectRatios()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.aspect_ratio),
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 16.dp),
            fontWeight = FontWeight.Medium
        )
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 4.dp,
                bottom = 4.dp,
                end = 16.dp + WindowInsets
                    .navigationBars
                    .asPaddingValues()
                    .calculateEndPadding(LocalLayoutDirection.current)
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(aspectRatios) { index, item ->
                if (item.aspectRatio != AspectRatio.Original) {
                    val selected = selectedIndex == index
                    AspectRatioSelectionCard(
                        modifier = Modifier
                            .width(90.dp)
                            .container(
                                resultPadding = 0.dp,
                                color = animateColorAsState(
                                    targetValue = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else MaterialTheme.colorScheme.surfaceContainerLowest,
                                ).value,
                                borderColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    0.7f
                                )
                                else MaterialTheme.colorScheme.outlineVariant()
                            )
                            .clickable { onAspectRatioChange(aspectRatios[index]) }
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 2.dp),
                        contentColor = Color.Transparent,
                        color = MaterialTheme.colorScheme.onSurface,
                        cropAspectRatio = item
                    )
                } else {
                    val selected = selectedIndex == index
                    Box(
                        modifier = Modifier
                            .container(
                                resultPadding = 0.dp,
                                color = animateColorAsState(
                                    targetValue = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else MaterialTheme.colorScheme.surfaceContainerLowest,
                                ).value,
                                borderColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    0.7f
                                )
                                else MaterialTheme.colorScheme.outlineVariant()
                            )
                            .clickable { onAspectRatioChange(aspectRatios[index]) }
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(Icons.Outlined.Image, null)
                            Text(
                                text = item.title,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}