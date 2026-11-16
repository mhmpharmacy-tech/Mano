package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ManoImageStudioVM"
private const val IMAGE_MODEL_NAME = "gemini-nano-banana-2.1"
private const val AI_IMAGE_TIMEOUT_MS = 60_000L

data class CareCardPreset(
  val id: String,
  val titleFa: String,
  val promptFa: String,
)

data class GeneratedCareCard(
  val id: String = UUID.randomUUID().toString(),
  val titleFa: String,
  val promptFa: String,
  val bitmap: Bitmap,
  val isEditedFromExisting: Boolean = false,
)

sealed interface ImageStudioUiState {
  val gallery: List<GeneratedCareCard>

  data class Idle(
    override val gallery: List<GeneratedCareCard> = emptyList(),
  ) : ImageStudioUiState

  data class Loading(
    val isEditing: Boolean,
    val statusTextFa: String,
    override val gallery: List<GeneratedCareCard>,
  ) : ImageStudioUiState

  data class Success(
    val activeCard: GeneratedCareCard,
    override val gallery: List<GeneratedCareCard>,
  ) : ImageStudioUiState

  data class Error(
    val messageFa: String,
    override val gallery: List<GeneratedCareCard>,
  ) : ImageStudioUiState
}

interface CareCardImageEngine {
  suspend fun generateFromPrompt(prompt: String): Bitmap?
  suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap?
}

class FirebaseCareCardImageEngine : CareCardImageEngine {
  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = IMAGE_MODEL_NAME,
      generationConfig =
        generationConfig {
          responseModalities = listOf(ResponseModality.IMAGE)
        },
    )
  }

  override suspend fun generateFromPrompt(prompt: String): Bitmap? {
    val response = model.generateContent(prompt)
    return response.candidates.firstOrNull()
      ?.content
      ?.parts
      ?.filterIsInstance<ImagePart>()
      ?.firstOrNull()
      ?.image
  }

  override suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap? {
    val response =
      model.generateContent(
        content {
          image(inputBitmap)
          text(editInstruction)
        }
      )
    return response.candidates.firstOrNull()
      ?.content
      ?.parts
      ?.filterIsInstance<ImagePart>()
      ?.firstOrNull()
      ?.image
  }
}

class ManoImageStudioViewModel(
  private val engine: CareCardImageEngine = FirebaseCareCardImageEngine(),
) : ViewModel() {

  val presets: List<CareCardPreset> =
    listOf(
      CareCardPreset(
        id = "bedtime",
        titleFa = "کارت روتین خواب آرام",
        promptFa = "پوستر آموزشی کودکانه با پالت پاستلی سفید و آبی فیروزه‌ای برای روتین خواب آرام کودک: مسواک زدن، کتاب قصه کوتاه و چراغ خواب ملایم",
      ),
      CareCardPreset(
        id = "handwash",
        titleFa = "کارت شست‌وشوی دست‌ها",
        promptFa = "تصویرسازی آموزشی گرم و کودک‌پسند درباره شستن دست‌ها با صابون و حباب‌های شاد پیش از غذا با پس‌زمینه تمیز و روشن",
      ),
      CareCardPreset(
        id = "hydration",
        titleFa = "کارت نوشیدن آب و میوه",
        promptFa = "کارت تصویری کودکانه برای تشویق کودک به نوشیدن آب و خوردن میوه‌های رنگارنگ در زمان تب یا گرما با سبک کتاب داستان ملایم",
      ),
      CareCardPreset(
        id = "medicine_buddy",
        titleFa = "کارت آرامش هنگام دارو",
        promptFa = "تصویرسازی لطیف و دلگرم‌کننده یک خرس مهربان پزشک با سرنگ خوراکی بدون سوزن و دفترچه ثبت زمان دارو با رنگ‌های نعنایی و کرم",
      ),
    )

  private val _uiState = MutableStateFlow<ImageStudioUiState>(ImageStudioUiState.Idle())
  val uiState: StateFlow<ImageStudioUiState> = _uiState.asStateFlow()

  private val _pickedReferenceBitmap = MutableStateFlow<Bitmap?>(null)
  val pickedReferenceBitmap: StateFlow<Bitmap?> = _pickedReferenceBitmap.asStateFlow()

  private var studioJob: Job? = null

  fun setPickedReferenceBitmap(bitmap: Bitmap?) {
    _pickedReferenceBitmap.value = bitmap
  }

  fun selectCardFromGallery(cardId: String) {
    studioJob?.cancel()
    studioJob = null
    val currentGallery = _uiState.value.gallery
    val target = currentGallery.firstOrNull { it.id == cardId } ?: return
    _uiState.value = ImageStudioUiState.Success(activeCard = target, gallery = currentGallery)
  }

  fun generateCard(rawPrompt: String, titleHintFa: String? = null) {
    val cleanPrompt = rawPrompt.trim()
    if (cleanPrompt.isEmpty()) {
      _uiState.value =
        ImageStudioUiState.Error(
          messageFa = "لطفاً ابتدا توضیح یا موضوع کارت آموزشی را بنویسید.",
          gallery = _uiState.value.gallery,
        )
      return
    }

    val currentGallery = _uiState.value.gallery
    val reference = _pickedReferenceBitmap.value
    val isEditing = reference != null

    studioJob?.cancel()
    studioJob = viewModelScope.launch {
      _uiState.value =
        ImageStudioUiState.Loading(
          isEditing = isEditing,
          statusTextFa =
            if (isEditing) {
              "در حال ویرایش تصویر انتخاب‌شده با هوش مصنوعی (حدود ۳۰ تا ۶۰ ثانیه)..."
            } else {
              "در حال ساخت کارت آموزشی با هوش مصنوعی (حدود ۳۰ تا ۶۰ ثانیه)..."
            },
          gallery = currentGallery,
        )

      try {
        val engineeredPrompt = buildPediatricPrompt(cleanPrompt, isEdit = isEditing)
        val generated =
          withTimeout(AI_IMAGE_TIMEOUT_MS) {
            if (reference != null) {
              engine.editWithImage(reference, engineeredPrompt)
            } else {
              engine.generateFromPrompt(engineeredPrompt)
            }
          }

        if (generated != null) {
          val card =
            GeneratedCareCard(
              titleFa = titleHintFa?.takeIf { it.isNotBlank() } ?: cleanPrompt.take(42),
              promptFa = cleanPrompt,
              bitmap = generated,
              isEditedFromExisting = isEditing,
            )
          val updatedGallery = listOf(card) + currentGallery
          _uiState.value = ImageStudioUiState.Success(activeCard = card, gallery = updatedGallery)
        } else {
          _uiState.value =
            ImageStudioUiState.Error(
              messageFa = "مدل تصویری خروجی قابل نمایش برنگرداند. لطفاً عبارت ساده‌تری امتحان کنید.",
              gallery = currentGallery,
            )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Image generation timed out after 60s", e)
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "زمان ساخت تصویر به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره تلاش کنید.",
            gallery = currentGallery,
          )
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Firebase AI image quota exceeded", e)
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "سهمیه یا محدودیت درخواست سرویس هوش مصنوعی تکمیل شده است (نیازمند بررسی پلن Blaze).",
            gallery = currentGallery,
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI image synthesis failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای نامشخص در ارتباط با سرور"
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "ساخت یا ویرایش تصویر انجام نشد: $detail",
            gallery = currentGallery,
          )
      }
    }
  }

  fun editActiveCard(editInstruction: String) {
    val active = (_uiState.value as? ImageStudioUiState.Success)?.activeCard
    if (active == null) {
      _uiState.value =
        ImageStudioUiState.Error(
          messageFa = "ابتدا یک کارت بسازید یا از گالری انتخاب کنید تا قابل ویرایش باشد.",
          gallery = _uiState.value.gallery,
        )
      return
    }
    val cleanEdit = editInstruction.trim()
    if (cleanEdit.isEmpty()) {
      _uiState.value =
        ImageStudioUiState.Error(
          messageFa = "لطفاً دستور ویرایش (مثلاً تغییر رنگ پس‌زمینه یا افزودن المان کودکانه) را بنویسید.",
          gallery = _uiState.value.gallery,
        )
      return
    }

    val currentGallery = _uiState.value.gallery
    studioJob?.cancel()
    studioJob = viewModelScope.launch {
      _uiState.value =
        ImageStudioUiState.Loading(
          isEditing = true,
          statusTextFa = "در حال اعمال ویرایش هوشمند روی کارت فعلی...",
          gallery = currentGallery,
        )

      try {
        val editedBitmap =
          withTimeout(AI_IMAGE_TIMEOUT_MS) {
            engine.editWithImage(active.bitmap, buildPediatricPrompt(cleanEdit, isEdit = true))
          }
        if (editedBitmap != null) {
          val editedCard =
            GeneratedCareCard(
              titleFa = "${active.titleFa} (ویرایش‌شده)",
              promptFa = cleanEdit,
              bitmap = editedBitmap,
              isEditedFromExisting = true,
            )
          val updatedGallery = listOf(editedCard) + currentGallery
          _uiState.value = ImageStudioUiState.Success(activeCard = editedCard, gallery = updatedGallery)
        } else {
          _uiState.value =
            ImageStudioUiState.Error(
              messageFa = "تصویر ویرایش‌شده دریافت نشد. لطفاً دوباره امتحان کنید.",
              gallery = currentGallery,
            )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Image edit timed out after 60s", e)
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "زمان ویرایش تصویر به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره تلاش کنید.",
            gallery = currentGallery,
          )
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Firebase AI image edit quota exceeded", e)
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "سهمیه سرویس تصویر هوش مصنوعی تکمیل شده است.",
            gallery = currentGallery,
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI image edit failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای نامشخص"
        _uiState.value =
          ImageStudioUiState.Error(
            messageFa = "ویرایش کارت انجام نشد: $detail",
            gallery = currentGallery,
          )
      }
    }
  }

  companion object {
    fun buildPediatricPrompt(userPrompt: String, isEdit: Boolean): String {
      return if (isEdit) {
        "Edit this child-friendly educational care card cleanly while keeping warm Scandinavian pastel colors, soft shapes, and safe family-friendly pediatric visual clarity: $userPrompt"
      } else {
        "Create a clean, warm Scandinavian pastel pediatric educational care card illustration for parents and children, bright airy white background, soft teal/mint/coral watercolor-vector style, gentle friendly visual storytelling, no scary imagery, no clinical prescriptions: $userPrompt"
      }
    }

    fun provideFactory(engine: CareCardImageEngine = FirebaseCareCardImageEngine()): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ManoImageStudioViewModel(engine) }
      }
  }
}
