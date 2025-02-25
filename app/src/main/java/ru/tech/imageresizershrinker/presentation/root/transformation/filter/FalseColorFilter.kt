package ru.tech.imageresizershrinker.presentation.root.transformation.filter

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFalseColorFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.domain.image.filters.Filter


class FalseColorFilter(
    private val context: Context,
    override val value: Pair<Color, Color> = Color(0f, 0f, 0.5f) to Color(1f, 0f, 0f),
) : FilterTransformation<Pair<Color, Color>>(
    context = context,
    title = R.string.false_color,
    value = value,
), Filter.FalseColor<Bitmap, Color> {
    override val cacheKey: String
        get() = (value to context).hashCode().toString()

    override fun createFilter(): GPUImageFilter = GPUImageFalseColorFilter(
        value.first.red,
        value.first.green,
        value.first.blue,
        value.second.red,
        value.second.green,
        value.second.blue
    )
}