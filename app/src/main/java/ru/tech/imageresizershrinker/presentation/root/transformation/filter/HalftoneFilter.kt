package ru.tech.imageresizershrinker.presentation.root.transformation.filter

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHalftoneFilter
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.domain.image.filters.Filter
import ru.tech.imageresizershrinker.domain.image.filters.FilterParam


class HalftoneFilter(
    private val context: Context,
    override val value: Float = 0.005f,
) : FilterTransformation<Float>(
    context = context,
    title = R.string.halftone,
    value = value,
    paramsInfo = listOf(
        FilterParam(valueRange = 0.001f..0.02f, roundTo = 4)
    )
), Filter.Halftone<Bitmap> {
    override val cacheKey: String
        get() = (value to context).hashCode().toString()

    override fun createFilter(): GPUImageFilter = GPUImageHalftoneFilter(value)
}