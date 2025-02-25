package ru.tech.imageresizershrinker.presentation.filters_screen.viewModel


import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tech.imageresizershrinker.domain.image.ImageManager
import ru.tech.imageresizershrinker.domain.model.ImageData
import ru.tech.imageresizershrinker.domain.model.ImageFormat
import ru.tech.imageresizershrinker.domain.model.ImageInfo
import ru.tech.imageresizershrinker.domain.model.ResizeType
import ru.tech.imageresizershrinker.domain.saving.FileController
import ru.tech.imageresizershrinker.domain.saving.SaveResult
import ru.tech.imageresizershrinker.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.presentation.root.transformation.filter.FilterTransformation
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val fileController: FileController,
    private val imageManager: ImageManager<Bitmap, ExifInterface>
) : ViewModel() {

    private val _bitmapSize = mutableStateOf<Long?>(null)
    val bitmapSize by _bitmapSize

    private val _canSave = mutableStateOf(false)
    val canSave by _canSave

    private val _uris = mutableStateOf<List<Uri>?>(null)
    val uris by _uris

    private val _bitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val bitmap: Bitmap? by _bitmap

    private val _keepExif = mutableStateOf(false)
    val keepExif by _keepExif

    private val _isImageLoading: MutableState<Boolean> = mutableStateOf(false)
    val isImageLoading: Boolean by _isImageLoading

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _previewBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val previewBitmap: Bitmap? by _previewBitmap

    private val _done: MutableState<Int> = mutableIntStateOf(0)
    val done by _done

    private val _selectedUri: MutableState<Uri?> = mutableStateOf(null)
    val selectedUri by _selectedUri

    private val _imageInfo = mutableStateOf(ImageInfo())
    val imageInfo by _imageInfo

    private val _filterList = mutableStateOf(listOf<FilterTransformation<*>>())
    val filterList by _filterList

    private val _needToApplyFilters = mutableStateOf(true)
    val needToApplyFilters by _needToApplyFilters

    fun setMime(imageFormat: ImageFormat) {
        _imageInfo.value = _imageInfo.value.copy(imageFormat = imageFormat)
        updatePreview()
    }

    fun updateUris(uris: List<Uri>?) {
        _uris.value = null
        _uris.value = uris
        _selectedUri.value = uris?.firstOrNull()
    }

    fun updateUrisSilently(removedUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _uris.value = uris
                if (_selectedUri.value == removedUri) {
                    val index = uris?.indexOf(removedUri) ?: -1
                    if (index == 0) {
                        uris?.getOrNull(1)?.let {
                            _selectedUri.value = it
                            _bitmap.value = imageManager.getImageWithTransformations(
                                uri = it.toString(),
                                transformations = filterList,
                                originalSize = false
                            )?.image
                        }
                    } else {
                        uris?.getOrNull(index - 1)?.let {
                            _selectedUri.value = it
                            _bitmap.value = imageManager.getImageWithTransformations(
                                uri = it.toString(),
                                transformations = filterList,
                                originalSize = false
                            )?.image
                        }
                    }
                }
                val u = _uris.value?.toMutableList()?.apply {
                    remove(removedUri)
                }
                _uris.value = u
            }
        }
    }

    fun updateBitmap(bitmap: Bitmap?) {
        viewModelScope.launch {
            _isImageLoading.value = true
            _bitmap.value = imageManager.scaleUntilCanShow(bitmap)?.upscale()
            _previewBitmap.value = bitmap?.let {
                imageManager.transform(
                    image = it,
                    transformations = filterList,
                    originalSize = false
                )?.let { image ->
                    imageManager.createPreview(
                        image = image,
                        imageInfo = imageInfo.copy(
                            width = image.width,
                            height = image.height
                        ),
                        onGetByteCount = { size ->
                            _bitmapSize.value = size.toLong()
                        }
                    )
                }
            } ?: _bitmap.value
            _isImageLoading.value = false
        }
    }

    fun setKeepExif(boolean: Boolean) {
        _keepExif.value = boolean
    }

    private var savingJob: Job? = null

    fun saveBitmaps(
        onResult: (Int, String) -> Unit
    ) = viewModelScope.launch {
        _isSaving.value = true
        withContext(Dispatchers.IO) {
            var failed = 0
            _done.value = 0
            uris?.forEach { uri ->
                runCatching {
                    imageManager.getImageWithTransformations(uri.toString(), filterList)?.image
                }.getOrNull()?.let { bitmap ->
                    val localBitmap = bitmap

                    val result = fileController.save(
                        saveTarget = ImageSaveTarget<ExifInterface>(
                            imageInfo = imageInfo,
                            originalUri = uri.toString(),
                            sequenceNumber = _done.value + 1,
                            data = imageManager.compress(
                                ImageData(
                                    image = localBitmap,
                                    imageInfo = imageInfo.copy(
                                        width = localBitmap.width,
                                        height = localBitmap.height
                                    )
                                )
                            )
                        ), keepMetadata = keepExif
                    )
                    if (result is SaveResult.Error.MissingPermissions) {
                        return@withContext onResult(-1, "")
                    }
                } ?: {
                    failed += 1
                }
                _done.value += 1
            }
            onResult(failed, fileController.savingPath)
        }
        _isSaving.value = false
    }.also {
        savingJob?.cancel()
        savingJob = it
        _isSaving.value = false
    }

    fun setBitmap(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _isImageLoading.value = true
                updateBitmap(
                    imageManager.getImage(uri = uri.toString(), originalSize = false)?.image
                )
                _selectedUri.value = uri
                _isImageLoading.value = false
            }
        }
    }

    private fun updateCanSave() {
        _canSave.value = _bitmap.value != null && _filterList.value.isNotEmpty()
    }

    private var filterJob: Job? = null

    fun setFilteredPreview(bitmap: Bitmap) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(200L)
                _isImageLoading.value = true
                updateBitmap(bitmap)
                _isImageLoading.value = false
                _needToApplyFilters.value = false
            }
        }
    }

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
        _needToApplyFilters.value = true
    }

    fun updateOrder(value: List<FilterTransformation<*>>) {
        _filterList.value = value
        _needToApplyFilters.value = true
    }

    fun addFilter(filter: FilterTransformation<*>) {
        _filterList.value = _filterList.value + filter
        updateCanSave()
        _needToApplyFilters.value = true
    }

    fun removeFilterAtIndex(index: Int) {
        _filterList.value = _filterList.value.toMutableList().apply {
            removeAt(index)
        }
        updateCanSave()
        _needToApplyFilters.value = true
    }

    private suspend fun Bitmap.upscale(): Bitmap {
        return if (this.width * this.height < 2000 * 2000) {
            imageManager.resize(this, 2000, 2000, ResizeType.Flexible)!!
        } else this
    }

    fun decodeBitmapFromUri(uri: Uri, onError: (Throwable) -> Unit) {
        imageManager.getImageAsync(
            uri = uri.toString(),
            originalSize = true,
            onGetImage = {
                setBitmap(uri)
                setMime(it.imageInfo.imageFormat)
            },
            onError = onError
        )
    }

    fun canShow(): Boolean = bitmap?.let { imageManager.canShow(it) } ?: false

    fun shareBitmaps(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            imageManager.shareImages(
                uris = uris?.map { it.toString() } ?: emptyList(),
                imageLoader = { uri ->
                    imageManager.getImageWithTransformations(uri, filterList)
                },
                onProgressChange = {
                    if (it == -1) {
                        onComplete()
                        _isSaving.value = false
                        _done.value = 0
                    } else {
                        _done.value = it
                    }
                }
            )
        }.also {
            savingJob?.cancel()
            savingJob = it
            _isSaving.value = false
        }
    }

    fun getImageManager(): ImageManager<Bitmap, ExifInterface> = imageManager

    fun setQuality(fl: Float) {
        _imageInfo.value = _imageInfo.value.copy(quality = fl)
        updatePreview()
    }

    private fun updatePreview() {
        _bitmap.value?.let { bitmap ->
            filterJob?.cancel()
            filterJob = viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    _isImageLoading.value = true
                    updateBitmap(bitmap)
                    _isImageLoading.value = false
                    _needToApplyFilters.value = false
                }
            }
        }
    }

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
    }

}