package ru.tech.imageresizershrinker.presentation.single_edit_screen.viewModel

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tech.imageresizershrinker.domain.image.ImageManager
import ru.tech.imageresizershrinker.domain.image.Metadata
import ru.tech.imageresizershrinker.domain.model.ImageData
import ru.tech.imageresizershrinker.domain.model.ImageFormat
import ru.tech.imageresizershrinker.domain.model.ImageInfo
import ru.tech.imageresizershrinker.domain.model.Preset
import ru.tech.imageresizershrinker.domain.model.ResizeType
import ru.tech.imageresizershrinker.domain.saving.FileController
import ru.tech.imageresizershrinker.domain.saving.SaveResult
import ru.tech.imageresizershrinker.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.presentation.erase_background_screen.components.PathPaint
import ru.tech.imageresizershrinker.presentation.root.transformation.filter.FilterTransformation
import javax.inject.Inject

@HiltViewModel
class SingleEditViewModel @Inject constructor(
    private val fileController: FileController,
    private val imageManager: ImageManager<Bitmap, ExifInterface>
) : ViewModel() {

    private val _erasePaths = mutableStateOf(listOf<PathPaint>())
    val erasePaths: List<PathPaint> by _erasePaths

    private val _eraseLastPaths = mutableStateOf(listOf<PathPaint>())
    val eraseLastPaths: List<PathPaint> by _eraseLastPaths

    private val _eraseUndonePaths = mutableStateOf(listOf<PathPaint>())
    val eraseUndonePaths: List<PathPaint> by _eraseUndonePaths

    private val _drawPaths = mutableStateOf(listOf<PathPaint>())
    val drawPaths: List<PathPaint> by _drawPaths

    private val _drawLastPaths = mutableStateOf(listOf<PathPaint>())
    val drawLastPaths: List<PathPaint> by _drawLastPaths

    private val _drawUndonePaths = mutableStateOf(listOf<PathPaint>())
    val drawUndonePaths: List<PathPaint> by _drawUndonePaths

    private val _filterList = mutableStateOf(listOf<FilterTransformation<*>>())
    val filterList by _filterList

    private val _cropProperties = mutableStateOf(
        CropDefaults.properties(
            cropOutlineProperty = CropOutlineProperty(
                OutlineType.Rect,
                RectCropShape(
                    id = 0,
                    title = OutlineType.Rect.name
                )
            ),
            fling = true
        )
    )
    val cropProperties by _cropProperties

    private val _exif: MutableState<ExifInterface?> = mutableStateOf(null)
    val exif by _exif

    private val _uri: MutableState<Uri> = mutableStateOf(Uri.EMPTY)
    val uri: Uri by _uri

    private val _internalBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val initialBitmap by _internalBitmap

    private val _bitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val bitmap: Bitmap? by _bitmap

    private val _previewBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val previewBitmap: Bitmap? by _previewBitmap

    private val _imageInfo: MutableState<ImageInfo> = mutableStateOf(ImageInfo())
    val imageInfo: ImageInfo by _imageInfo

    private val _isImageLoading: MutableState<Boolean> = mutableStateOf(false)
    val isImageLoading: Boolean by _isImageLoading

    private val _showWarning: MutableState<Boolean> = mutableStateOf(false)
    val showWarning: Boolean by _showWarning

    private val _shouldShowPreview: MutableState<Boolean> = mutableStateOf(true)
    val shouldShowPreview by _shouldShowPreview

    private val _presetSelected: MutableState<Preset> = mutableStateOf(Preset.None)
    val presetSelected by _presetSelected

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving by _isSaving

    private var job: Job? = null

    private fun checkBitmapAndUpdate(resetPreset: Boolean, resetTelegram: Boolean) {
        if (resetPreset || resetTelegram) {
            _presetSelected.value = Preset.None
        }
        job?.cancel()
        _isImageLoading.value = false
        job = viewModelScope.launch {
            _isImageLoading.value = true
            delay(600)
            _bitmap.value?.let { bmp ->
                val preview = updatePreview(bmp)
                _previewBitmap.value = null
                _shouldShowPreview.value = imageManager.canShow(preview)
                if (shouldShowPreview) _previewBitmap.value = preview

                _imageInfo.value = _imageInfo.value.run {
                    if (resizeType is ResizeType.Ratio) copy(
                        height = preview.height,
                        width = preview.width
                    ) else this
                }
            }
            _isImageLoading.value = false
        }
    }

    private var savingJob: Job? = null
    fun saveBitmap(
        onComplete: (result: SaveResult) -> Unit,
    ) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            _isSaving.value = true
            bitmap?.let { bitmap ->
                onComplete(
                    fileController.save(
                        saveTarget = ImageSaveTarget(
                            imageInfo = imageInfo,
                            metadata = exif,
                            originalUri = uri.toString(),
                            sequenceNumber = null,
                            data = imageManager.compress(
                                ImageData(
                                    image = bitmap,
                                    imageInfo = imageInfo,
                                    metadata = exif,
                                )
                            )
                        ),
                        keepMetadata = true
                    )
                )
            }
            _isSaving.value = false
        }
    }.also {
        _isSaving.value = false
        savingJob?.cancel()
        savingJob = it
    }

    private suspend fun updatePreview(
        bitmap: Bitmap
    ): Bitmap = withContext(Dispatchers.IO) {
        return@withContext imageInfo.run {
            _showWarning.value = width * height * 4L >= 10_000 * 10_000 * 3L
            imageManager.createPreview(
                image = bitmap,
                imageInfo = this,
                onGetByteCount = {
                    _imageInfo.value = _imageInfo.value.copy(sizeInBytes = it)
                }
            )
        }
    }

    private fun setBitmapInfo(newInfo: ImageInfo) {
        if (_imageInfo.value != newInfo || _imageInfo.value.quality == 100f) {
            _imageInfo.value = newInfo
            checkBitmapAndUpdate(resetPreset = false, resetTelegram = true)
        }
    }

    fun resetValues(newBitmapComes: Boolean = false) {
        _imageInfo.value = ImageInfo(
            width = _internalBitmap.value?.width ?: 0,
            height = _internalBitmap.value?.height ?: 0,
            imageFormat = imageInfo.imageFormat
        )
        if (newBitmapComes) {
            _bitmap.value = _internalBitmap.value
        }
        checkBitmapAndUpdate(resetPreset = true, resetTelegram = true)
    }

    fun updateBitmap(bitmap: Bitmap?) {
        viewModelScope.launch {
            val size = bitmap?.let { bitmap.width to bitmap.height }
            _bitmap.value =
                imageManager.scaleUntilCanShow(bitmap).also { _internalBitmap.value = it }
            resetValues(true)
            _imageInfo.value = _imageInfo.value.copy(
                width = size?.first ?: 0,
                height = size?.second ?: 0
            )
        }
    }

    fun updateBitmapAfterEditing(bitmap: Bitmap?, saveOriginalSize: Boolean = false) {
        viewModelScope.launch {
            val bmp = bitmap?.let {
                imageManager.resize(
                    image = it,
                    width = (if (imageInfo.rotationDegrees % 90 != 0f || imageInfo.rotationDegrees == 0f) _bitmap.value?.width else _bitmap.value?.height)
                        ?: it.width,
                    height = (if (imageInfo.rotationDegrees % 90 != 0f || imageInfo.rotationDegrees == 0f) _bitmap.value?.height else _bitmap.value?.width)
                        ?: it.height,
                    resizeType = if (saveOriginalSize) ResizeType.Explicit else ResizeType.Flexible
                )
            }
            val size = bmp?.let { bmp.width to bmp.height }
            _bitmap.value = imageManager.scaleUntilCanShow(bmp)
            resetValues()
            _imageInfo.value = _imageInfo.value.copy(
                width = size?.first ?: 0,
                height = size?.second ?: 0
            )
        }
    }

    fun rotateLeft() {
        _imageInfo.value = _imageInfo.value.run {
            copy(
                rotationDegrees = _imageInfo.value.rotationDegrees - 90f,
                height = width,
                width = height
            )
        }
        checkBitmapAndUpdate(resetPreset = false, resetTelegram = false)
    }

    fun rotateRight() {
        _imageInfo.value = _imageInfo.value.run {
            copy(
                rotationDegrees = _imageInfo.value.rotationDegrees + 90f,
                height = width,
                width = height
            )
        }
        checkBitmapAndUpdate(resetPreset = false, resetTelegram = false)
    }

    fun flip() {
        _imageInfo.value = _imageInfo.value.copy(isFlipped = !_imageInfo.value.isFlipped)
        checkBitmapAndUpdate(resetPreset = false, resetTelegram = false)
    }

    fun updateWidth(width: Int) {
        if (_imageInfo.value.width != width) {
            _imageInfo.value = _imageInfo.value.copy(width = width)
            checkBitmapAndUpdate(resetPreset = true, resetTelegram = true)
        }
    }

    fun updateHeight(height: Int) {
        if (_imageInfo.value.height != height) {
            _imageInfo.value = _imageInfo.value.copy(height = height)
            checkBitmapAndUpdate(resetPreset = true, resetTelegram = true)
        }
    }

    fun setQuality(quality: Float) {
        if (_imageInfo.value.quality != quality) {
            _imageInfo.value = _imageInfo.value.copy(quality = quality.coerceIn(0f, 100f))
            checkBitmapAndUpdate(resetPreset = false, resetTelegram = false)
        }
    }

    fun setMime(imageFormat: ImageFormat) {
        if (_imageInfo.value.imageFormat != imageFormat) {
            _imageInfo.value = _imageInfo.value.copy(imageFormat = imageFormat)
            if (imageFormat != ImageFormat.Png) checkBitmapAndUpdate(
                resetPreset = false,
                resetTelegram = true
            )
            else checkBitmapAndUpdate(resetPreset = false, resetTelegram = false)
        }
    }

    fun setResizeType(type: ResizeType) {
        if (_imageInfo.value.resizeType != type) {
            _imageInfo.value = _imageInfo.value.copy(resizeType = type)
            if (type != ResizeType.Ratio) checkBitmapAndUpdate(
                resetPreset = false,
                resetTelegram = false
            )
            else checkBitmapAndUpdate(resetPreset = false, resetTelegram = true)
        }
    }

    fun setUri(uri: Uri) {
        _uri.value = uri
    }

    fun decodeBitmapByUri(
        uri: Uri,
        originalSize: Boolean = true,
        onGetMimeType: (ImageFormat) -> Unit,
        onGetExif: (ExifInterface?) -> Unit,
        onGetBitmap: (Bitmap) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        imageManager.getImageAsync(
            uri = uri.toString(),
            originalSize = originalSize,
            onGetImage = {
                onGetBitmap(it.image)
                onGetExif(it.metadata)
                onGetMimeType(it.imageInfo.imageFormat)
            },
            onError = onError
        )
    }

    fun shareBitmap(onComplete: () -> Unit) {
        _isSaving.value = false
        savingJob?.cancel()
        savingJob = viewModelScope.launch {
            _isSaving.value = true
            imageManager.shareImages(
                uris = listOf(_uri.value.toString()),
                imageLoader = {
                    imageManager.getImage(uri = uri.toString())?.image?.let {
                        ImageData(
                            it,
                            imageInfo
                        )
                    }
                },
                onProgressChange = { onComplete() }
            )
            _isSaving.value = false
        }
    }

    fun canShow(): Boolean = bitmap?.let { imageManager.canShow(it) } ?: false

    fun setPreset(preset: Preset) {
        setBitmapInfo(
            imageManager.applyPresetBy(
                image = bitmap,
                preset = preset,
                currentInfo = imageInfo
            )
        )
        _presetSelected.value = preset
    }

    fun clearExif() {
        val t = _exif.value
        Metadata.metaTags.forEach {
            t?.setAttribute(it, null)
        }
        _exif.value = t
    }

    fun updateExif(exifInterface: ExifInterface?) {
        _exif.value = exifInterface
    }

    fun removeExifTag(tag: String) {
        val exifInterface = _exif.value
        exifInterface?.setAttribute(tag, null)
        updateExif(exifInterface)
    }

    fun updateExifByTag(tag: String, value: String) {
        val exifInterface = _exif.value
        exifInterface?.setAttribute(tag, value)
        updateExif(exifInterface)
    }

    fun setCropAspectRatio(aspectRatio: AspectRatio) {
        _cropProperties.value = _cropProperties.value.copy(
            aspectRatio = aspectRatio,
            fixedAspectRatio = aspectRatio != AspectRatio.Original
        )
    }

    fun setCropMask(cropOutlineProperty: CropOutlineProperty) {
        _cropProperties.value =
            _cropProperties.value.copy(cropOutlineProperty = cropOutlineProperty)
    }

    suspend fun loadImage(uri: Uri): Bitmap? = imageManager.getImage(data = uri)

    fun getImageManager(): ImageManager<Bitmap, ExifInterface> = imageManager

    fun <T : Any> updateFilter(
        value: T,
        index: Int,
        showError: (Throwable) -> Unit
    ) {
        val list = _filterList.value.toMutableList()
        kotlin.runCatching {
            list[index] = list[index].copy(value)
            _filterList.value = list
        }.exceptionOrNull()?.let {
            showError(it)
            list[index] = list[index].newInstance()
            _filterList.value = list
        }
    }

    fun updateOrder(value: List<FilterTransformation<*>>) {
        _filterList.value = value
    }

    fun addFilter(filter: FilterTransformation<*>) {
        _filterList.value = _filterList.value + filter
    }

    fun removeFilterAtIndex(index: Int) {
        _filterList.value = _filterList.value.toMutableList().apply {
            removeAt(index)
        }
    }

    fun clearFilterList() {
        _filterList.value = listOf()
    }

    fun calculateScreenOrientationBasedOnBitmap(bitmap: Bitmap?): Int {
        if (bitmap == null) return ActivityInfo.SCREEN_ORIENTATION_USER
        val imageRatio = bitmap.width / bitmap.height.toFloat()
        return if (imageRatio <= 1.05f) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun clearDrawing(canUndo: Boolean = false) {
        viewModelScope.launch {
            delay(500L)
            _drawLastPaths.value = if (canUndo) drawPaths else listOf()
            _drawPaths.value = listOf()
            _drawUndonePaths.value = listOf()
        }
    }

    fun undoDraw() {
        if (drawPaths.isEmpty() && drawLastPaths.isNotEmpty()) {
            _drawPaths.value = drawLastPaths
            _drawLastPaths.value = listOf()
            return
        }
        if (drawPaths.isEmpty()) {
            return
        }
        val lastPath = drawPaths.lastOrNull()

        _drawPaths.value = drawPaths.toMutableList().apply {
            remove(lastPath)
        }
        if (lastPath != null) {
            _drawUndonePaths.value = drawUndonePaths.toMutableList().apply {
                add(lastPath)
            }
        }
    }

    fun redoDraw() {
        if (drawUndonePaths.isEmpty()) {
            return
        }
        val lastPath = drawUndonePaths.last()
        addPathToDrawList(lastPath)
        _drawUndonePaths.value = drawUndonePaths.toMutableList().apply {
            remove(lastPath)
        }
    }

    fun addPathToDrawList(pathPaint: PathPaint) {
        _drawPaths.value = _drawPaths.value.toMutableList().apply {
            add(pathPaint)
        }
        _drawUndonePaths.value = listOf()
    }

    fun clearErasing(canUndo: Boolean = false) {
        viewModelScope.launch {
            delay(250L)
            _eraseLastPaths.value = if (canUndo) erasePaths else listOf()
            _erasePaths.value = listOf()
            _eraseUndonePaths.value = listOf()
        }
    }

    fun undoErase() {
        if (erasePaths.isEmpty() && eraseLastPaths.isNotEmpty()) {
            _erasePaths.value = eraseLastPaths
            _eraseLastPaths.value = listOf()
            return
        }
        if (erasePaths.isEmpty()) {
            return
        }
        val lastPath = erasePaths.lastOrNull()

        _erasePaths.value = erasePaths.toMutableList().apply {
            remove(lastPath)
        }
        if (lastPath != null) {
            _eraseUndonePaths.value = eraseUndonePaths.toMutableList().apply {
                add(lastPath)
            }
        }
    }

    fun redoErase() {
        if (eraseUndonePaths.isEmpty()) {
            return
        }
        val lastPath = eraseUndonePaths.last()
        addPathToEraseList(lastPath)
        _eraseUndonePaths.value = eraseUndonePaths.toMutableList().apply {
            remove(lastPath)
        }
    }

    fun addPathToEraseList(pathPaint: PathPaint) {
        _erasePaths.value = _erasePaths.value.toMutableList().apply {
            add(pathPaint)
        }
        _eraseUndonePaths.value = listOf()
    }

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
    }

}
