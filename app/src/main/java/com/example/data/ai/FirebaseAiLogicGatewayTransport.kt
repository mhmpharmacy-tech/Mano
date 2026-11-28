package com.example.data.ai

import android.graphics.BitmapFactory
import android.util.Log
import com.example.domain.ai.AiGatewayTransport
import com.example.domain.ai.AiTaskSchema
import com.example.domain.ai.AiTransientTransportException
import com.example.domain.ai.AiTransportRequest
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

/**
 * Production transport backed exclusively by Firebase AI Logic + App Check attestation
 * (zero client-side or hardcoded Gemini API keys).
 */
class FirebaseAiLogicGatewayTransport : AiGatewayTransport {
  private val modelCache = ConcurrentHashMap<Pair<String, AiTaskSchema>, GenerativeModel>()

  private fun getOrCreateModel(
    modelId: String,
    taskSchema: AiTaskSchema,
    systemInstructionText: String,
  ): GenerativeModel {
    val key = modelId to taskSchema
    return modelCache.getOrPut(key) {
      val config =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = schemaFor(taskSchema)
          temperature = 0.2f
        }
      Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = modelId,
        generationConfig = config,
        systemInstruction = content { text(systemInstructionText) },
      )
    }
  }

  override suspend fun generateStructuredJson(request: AiTransportRequest): String {
    val model = getOrCreateModel(request.modelId, request.taskSchema, request.systemInstruction)
    try {
      val inputContent =
        content {
          text(request.userPrompt)
          val bytes = request.jpegImageBytes
          if (bytes != null && bytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
              image(bitmap)
            }
          }
        }
      val response = model.generateContent(inputContent)
      return response.text?.trim().orEmpty()
    } catch (e: CancellationException) {
      throw e
    } catch (e: IOException) {
      Log.e(TAG, "Transient network error in Firebase AI Logic gateway", e)
      throw AiTransientTransportException("Network error contacting AI backend", e)
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI Logic gateway call failed", e)
      throw AiTransientTransportException(e.localizedMessage ?: "Firebase AI Logic failure", e)
    }
  }

  private fun schemaFor(taskSchema: AiTaskSchema): Schema =
    when (taskSchema) {
      AiTaskSchema.SAFETY_CHECKLIST ->
        Schema.obj(
          properties = mapOf(
            "checklistTitleFa" to Schema.string(),
            "urgencySummaryFa" to Schema.string(),
            "immediateEmergencySignsFa" to Schema.array(Schema.string()),
            "sameDayPediatricianVisitSignsFa" to Schema.array(Schema.string()),
            "homeMonitoringChecklistFa" to Schema.array(Schema.string()),
            "questionsForPediatricianFa" to Schema.array(Schema.string()),
            "citedSourceIds" to Schema.array(Schema.string()),
          ),
        )
      AiTaskSchema.DOCUMENTED_QA ->
        Schema.obj(
          properties = mapOf(
            "directAnswerFa" to Schema.string(),
            "actionableStepsFa" to Schema.array(Schema.string()),
            "whenToVisitPediatricianFa" to Schema.array(Schema.string()),
            "citedSourceIds" to Schema.array(Schema.string()),
            "safetyDisclaimerFa" to Schema.string(),
          ),
        )
      AiTaskSchema.VISIT_DRAFT ->
        Schema.obj(
          properties = mapOf(
            "titleFa" to Schema.string(),
            "summaryParagraphFa" to Schema.string(),
            "redFlagsObservedFa" to Schema.array(Schema.string()),
            "questionsForClinicianFa" to Schema.array(Schema.string()),
            "clinicianReviewNoticeFa" to Schema.string(),
          ),
          optionalProperties = listOf("redFlagsObservedFa", "questionsForClinicianFa", "clinicianReviewNoticeFa"),
        )
      AiTaskSchema.NUTRITION_EDUCATION ->
        Schema.obj(
          properties = mapOf(
            "planTitleFa" to Schema.string(),
            "overviewFa" to Schema.string(),
            "growthNutritionFocusFa" to Schema.string(),
            "meals" to Schema.array(
              Schema.obj(
                properties = mapOf(
                  "mealTypeFa" to Schema.string(),
                  "titleFa" to Schema.string(),
                  "whySuitableFa" to Schema.string(),
                  "textureAndPortionFa" to Schema.string(),
                  "ingredientsFa" to Schema.array(Schema.string()),
                  "prepStepsFa" to Schema.array(Schema.string()),
                ),
                optionalProperties = listOf("whySuitableFa", "textureAndPortionFa", "prepStepsFa"),
              ),
            ),
            "hydrationTipFa" to Schema.string(),
            "safetyAvoidListFa" to Schema.array(Schema.string()),
            "doctorFollowUpHintFa" to Schema.string(),
            "citedSourceIds" to Schema.array(Schema.string()),
          ),
        )
      AiTaskSchema.SYRUP_LABEL_OCR ->
        Schema.obj(
          properties = mapOf(
            "drugNameFa" to Schema.string(),
            "activeIngredientKey" to Schema.enumeration(listOf("ACETAMINOPHEN", "IBUPROFEN", "CETIRIZINE", "UNKNOWN")),
            "dosageForm" to Schema.enumeration(listOf("DROPS", "SYRUP", "SUSPENSION", "UNKNOWN")),
            "mgAmount" to Schema.double(nullable = true),
            "perMlAmount" to Schema.double(nullable = true),
            "mgPerMl" to Schema.double(nullable = true),
            "confidenceLevel" to Schema.enumeration(listOf("HIGH", "MEDIUM", "LOW", "REQUIRE_REVIEW", "UNKNOWN")),
            "concentrationDisplayFa" to Schema.string(),
            "safetyNoteFa" to Schema.string(),
            "missingFields" to Schema.array(Schema.string()),
            "requiresHumanReview" to Schema.boolean(),
          ),
          optionalProperties = listOf("mgAmount", "perMlAmount", "mgPerMl", "missingFields"),
        )
    }

  companion object {
    private const val TAG = "ManoAiLogicTransport"
  }
}
