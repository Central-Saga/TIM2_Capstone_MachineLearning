# 📱 KitchenGuard CSM - Android Integration Guide (COMPLETE)

**Version**: 3.0  
**Status**: Ready to Implement  
**API Server**: http://localhost:8000 (or production URL)

---

## 🚀 Quick Start for Android Team

### Prerequisites
- Kotlin development environment configured
- Minimum API level: 21 (Android 5.0)
- Internet permission enabled
- Back-end server accessible from mobile network

---

## Step 1: Add Dependencies (`build.gradle`)

```groovy
dependencies {
    // Retrofit for REST API
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:gsonConverterFactory:2.9.0'
    
    // OkHttp for network logging
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutine support
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // ViewModel & LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
}
```

---

## Step 2: Create Data Models

### File: `app/src/main/java/com/kitchenguard/model/WasteResponse.kt`

```kotlin
package com.kitchenguard.model

import com.google.gson.annotations.SerializedName

// ========================================
// REQUEST MODELS
// ========================================

data class PredictRequest(
    val text: String,
    @SerializedName("threshold")
    val threshold: Double = 0.85,
    @SerializedName("estimated_weight_kg")
    val estimatedWeightKg: Float? = null
)

data class LossCalculationRequest(
    val category: String,
    @SerializedName("weight_kg")
    val weightKg: Float
)

data class DailyReportRequest(
    @SerializedName("waste_entries")
    val wasteEntries: List<WasteEntry>,
    @SerializedName("filter_category")
    val filterCategory: String? = null
)

data class WasteEntry(
    val category: String,
    @SerializedName("weight_kg")
    val weightKg: Float
)

// ========================================
// RESPONSE MODELS
// ========================================

data class ApiResponse<T>(
    val success: Boolean,
    val timestamp: String? = null,
    val data: T? = null,
    @SerializedName("processing_metadata")
    val processingMetadata: ProcessingMeta? = null
)

data class ProcessingMeta(
    @SerializedName("processing_time_ms")
    val processingTimeMs: Double,
    @SerializedName("model_version")
    val modelVersion: String
)

data class PredictionData(
    @SerializedName("predicted_class")
    val predictedClass: String,
    @SerializedName("confidence")
    val confidence: Float,
    @SerializedName("gate_status")
    val gateStatus: String,
    @SerializedName("action_recommendation")
    val actionRecommendation: String,
    @SerializedName("probability_distribution")
    val probabilityDistribution: Map<String, Float>? = null,
    @SerializedName("financial_impact")
    val financialImpact: LossData? = null
)

data class LossData(
    val category: String,
    @SerializedName("weight_kg")
    val weightKg: Float,
    @SerializedName("cost_per_kg_rupiah")
    val costPerKgRupiah: Int,
    @SerializedName("disposal_factor")
    val disposalFactor: Double,
    @SerializedName("ingredient_loss_rupiah")
    val ingredientLossRupiah: Double,
    @SerializedName("disposal_cost_rupiah")
    val disposalCostRupiah: Double,
    @SerializedName("total_loss_rupiah")
    val totalLossRupiah: Double,
    @SerializedName("priority_level")
    val priorityLevel: String,
    @SerializedName("action_recommendation")
    val actionRecommendation: String,
    val timestamp: String
)

data class DailyReportData(
    val report_date: String,
    val summary: ReportSummary,
    @SerializedName("category_breakdown")
    val categoryBreakdown: Map<String, CategoryStats>,
    @SerializedName("priority_distribution")
    val priorityDistribution: Map<String, Int>,
    @SerializedName("risk_assessment")
    val riskAssessment: RiskAssessment
)

data class ReportSummary(
    @SerializedName("total_entries")
    val totalEntries: Int,
    @SerializedName("total_weight_kg")
    val totalWeightKg: Float,
    @SerializedName("total_financial_loss_rupiah")
    val totalFinancialLossRupiah: Long,
    @SerializedName("average_loss_per_entry_rupiah")
    val averageLossPerEntryRupiah: Long
)

data class CategoryStats(
    @SerializedName("count")
    val count: Int,
    @SerializedName("total_weight_kg")
    val totalWeightKg: Float,
    @SerializedName("total_loss_rupiah")
    val totalLossRupiah: Double
)

data class RiskAssessment(
    val level: String,
    val message: String,
    @SerializedName("critical_incidents")
    val criticalIncidents: Int,
    @SerializedName("high_priority_incidents")
    val highPriorityIncidents: Int
)
```

---

## Step 3: Create Retrofit Service Interface

### File: `app/src/main/java/com/kitchenguard/api/KitchenGuardApiService.kt`

```kotlin
package com.kitchenguard.api

import com.kitchenguard.model.*
import retrofit2.Call
import retrofit2.http.*

interface KitchenGuardApiService {
    
    // Health Check
    @GET("api/health")
    fun getHealth(): Call<ApiResponse<HealthStatus>>
    
    // Waste Classification
    @POST("api/predict")
    fun classifyWaste(@Body request: PredictRequest): Call<ApiResponse<PredictionData>>
    
    // Financial Loss Calculation
    @POST("api/calculate-loss")
    fun calculateLoss(@Body request: LossCalculationRequest): Call<ApiResponse<LossData>>
    
    // Daily Summary Report
    @POST("api/reports/daily-summary")
    fun getDailyReport(@Body request: DailyReportRequest): Call<ApiResponse<DailyReportData>>
    
    // Get Model Info
    @GET("api/info")
    fun getModelInfo(): Call<ApiResponse<ModelInfo>>
}

data class HealthStatus(
    val status: String,
    val service: String,
    @SerializedName("ml_available")
    val mlAvailable: Boolean
)

data class ModelInfo(
    val model_name: String,
    val version: String,
    val categories: List<String>,
    val performance: Map<String, Any>,
    @SerializedName("training_stats")
    val trainingStats: Map<String, Any>
)
```

---

## Step 4: Create ApiClient Singleton

### File: `app/src/main/java/com/kitchenguard/utils/ApiClient.kt`

```kotlin
package com.kitchenguard.utils

import android.content.Context
import com.kitchenguard.api.KitchenGuardApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://YOUR_SERVER_IP:8000/" // Change to production
    
    private lateinit var apiService: KitchenGuardApiService
    
    fun initialize(context: Context) {
        if (!this::apiService.isInitialized) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            apiService = retrofit.create(KitchenGuardApiService::class.java)
        }
    }
    
    fun getService(): KitchenGuardApiService {
        require(this::apiService.isInitialized) { "ApiClient not initialized. Call ApiClient.initialize(context) first." }
        return apiService
    }
}
```

---

## Step 5: Create Repository Layer

### File: `app/src/main/java/com/kitchenguard/repository/WasteRepository.kt`

```kotlin
package com.kitchenguard.repository

import com.kitchenguard.api.KitchenGuardApiService
import com.kitchenguard.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import retrofit2.Response

class WasteRepository(private val apiService: KitchenGuardApiService) {
    
    suspend fun classifyWaste(
        text: String,
        weightKg: Float? = null,
        threshold: Double = 0.85
    ): Result<PredictionData> = try {
        val response = apiService.classifyWaste(
            PredictRequest(
                text = text,
                threshold = threshold,
                estimatedWeightKg = weightKg
            )
        ).execute()
        
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(response.body()!!.data!!)
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun calculateLoss(category: String, weightKg: Float): Result<LossData> = try {
        val response = apiService.calculateLoss(
            LossCalculationRequest(category = category, weightKg = weightKg)
        ).execute()
        
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(response.body()!!.data!!)
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getDailyReport(
        entries: List<WasteEntry>,
        filterCategory: String? = null
    ): Result<DailyReportData> = try {
        val response = apiService.getDailyReport(
            DailyReportRequest(
                wasteEntries = entries,
                filterCategory = filterCategory
            )
        ).execute()
        
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(response.body()!!.data!!)
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getHealth(): Result<HealthStatus> = try {
        val response = apiService.getHealth().execute()
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(response.body()!!.data!!)
        } else {
            Result.failure(Exception(response.message()))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## Step 6: Create ViewModel

### File: `app/src/main/java/com/kitchenguard/viewmodel/WasteClassifierViewModel.kt`

```kotlin
package com.kitchenguard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchenguard.model.*
import com.kitchenguard.repository.WasteRepository
import kotlinx.coroutines.launch

class WasteClassifierViewModel(private val repository: WasteRepository) : ViewModel() {
    
    private val _classificationResult = MutableLiveData<Result<PredictionData>>()
    val classificationResult: LiveData<Result<PredictionData>> = _classificationResult
    
    private val _dailyReport = MutableLiveData<Result<DailyReportData>>()
    val dailyReport: LiveData<Result<DailyReportData>> = _dailyReport
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun classifyWaste(text: String, weightKg: Float? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _classificationResult.value = repository.classifyWaste(text, weightKg)
            _isLoading.value = false
        }
    }
    
    fun getDailyReport(entries: List<WasteEntry>, filter: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _dailyReport.value = repository.getDailyReport(entries, filter)
            _isLoading.value = false
        }
    }
    
    fun getRiskLevelFromReport(report: DailyReportData): String {
        return report.riskAssessment.level
    }
    
    fun getActionRecommendation(prediction: PredictionData): String {
        return prediction.actionRecommendation
    }
    
    fun hasHighConfidence(prediction: PredictionData, threshold: Float = 0.85f): Boolean {
        return prediction.confidence >= threshold
    }
    
    fun getTotalLoss(report: DailyReportData): Long {
        return report.summary.totalFinancialLossRupiah
    }
    
    fun getCategoryBreakdown(report: DailyReportData): Map<String, CategoryStats> {
        return report.categoryBreakdown
    }
}
```

---

## Step 7: Usage Example in Activity

### File: `app/src/main/java/com/kitchenguard/ui/WasteClassificationActivity.kt`

```kotlin
package com.kitchenguard.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kitchenguard.databinding.ActivityWasteClassificationBinding
import com.kitchenguard.model.WasteEntry
import com.kitchenguard.repository.WasteRepository
import com.kitchenguard.utils.ApiClient
import com.kitchenguard.viewmodel.WasteClassifierViewModel

class WasteClassificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWasteClassificationBinding
    private lateinit var viewModel: WasteClassifierViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWasteClassificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize API client
        ApiClient.initialize(applicationContext)
        val repository = WasteRepository(ApiClient.getService())
        viewModel = ViewModelProvider(this)[WasteClassifierViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
        loadModelInfo()
    }
    
    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.classificationResult.observe(this) { result ->
            result.onSuccess { prediction ->
                displayPrediction(prediction)
            }.onFailure { error ->
                showToast("Classification failed: ${error.message}")
            }
        }
        
        viewModel.dailyReport.observe(this) { result ->
            result.onSuccess { report ->
                displayDailyReport(report)
            }.onFailure { error ->
                showToast("Report generation failed: ${error.message}")
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnPredict.setOnClickListener {
            val text = binding.etInputText.text.toString()
            val weight = binding.etWeight.text?.toString()?.toFloatOrNull()
            
            if (text.isNotBlank()) {
                viewModel.classifyWaste(text, weight)
                
                // Add to daily report tracking
                viewModel.classificationResult.value?.getOrNull()?.let { prediction ->
                    val newEntry = WasteEntry(
                        category = prediction.predictedClass,
                        weightKg = weight ?: 1.0f
                    )
                    trackDailyEntry(newEntry)
                }
            } else {
                showToast("Please enter waste description")
            }
        }
        
        binding.btnGenerateReport.setOnClickListener {
            viewModel.getDailyReport(currentDailyEntries)
        }
    }
    
    private fun displayPrediction(prediction: PredictionData) {
        binding.tvCategory.text = prediction.predictedClass
        
        val confidencePercent = (prediction.confidence * 100).toInt()
        binding.tvConfidence.text = "$confidencePercent%"
        
        binding.tvGateStatus.text = prediction.gateStatus
        
        binding.tvRecommendation.text = prediction.actionRecommendation
        
        prediction.financialImpact?.let { loss ->
            binding.cardFinancial.visibility = View.VISIBLE
            binding.tvTotalLoss.text = formatCurrency(loss.totalLossRupiah.toLong())
            binding.tvPriority.text = loss.priorityLevel
            binding.tvAction.text = loss.actionRecommendation
        }
    }
    
    private fun displayDailyReport(report: DailyReportData) {
        binding.tvReportDate.text = report.report_date
        binding.tvTotalEntries.text = "${report.summary.totalEntries} entries"
        binding.tvTotalWeight.text = "${report.summary.totalWeightKg} kg"
        binding.tvTotalLoss.text = formatCurrency(report.summary.totalFinancialLossRupiah)
        binding.tvAverageLoss.text = formatCurrency(report.summary.averageLossPerEntryRupiah)
        
        // Display risk assessment
        binding.tvRiskLevel.text = report.riskAssessment.level
        binding.tvRiskMessage.text = report.riskAssessment.message
        
        // Update risk color based on severity
        when (report.riskAssessment.level) {
            "CRITICAL" -> binding.tvRiskLevel.setTextColor(resources.getColor(R.color.red))
            "HIGH" -> binding.tvRiskLevel.setTextColor(resources.getColor(R.color.orange))
            else -> binding.tvRiskLevel.setTextColor(resources.getColor(R.color.green))
        }
    }
    
    private fun formatCurrency(amount: Long): String {
        return String.format("Rp %,d", amount)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun loadModelInfo() {
        // Load and display available categories
        viewModel.classificationResult.value?.getOrNull()?.probabilityDistribution?.keys?.let { categories ->
            binding.tvCategories.text = "Categories: ${categories.joinToString(", ")}"
        }
    }
    
    // Track entries for daily report
    private val currentDailyEntries = mutableListOf<WasteEntry>()
    
    private fun trackDailyEntry(entry: WasteEntry) {
        currentDailyEntries.add(entry)
        updateEntryCount()
    }
    
    private fun updateEntryCount() {
        binding.tvEntryCount.text = "${currentDailyEntries.size} entries tracked today"
    }
}
```

---

## Testing Checklist

- [ ] 1. Initialize ApiClient in Application class
- [ ] 2. Test health check endpoint
- [ ] 3. Test waste classification with sample text
- [ ] 4. Verify financial calculation integrates correctly
- [ ] 5. Test daily report generation
- [ ] 6. Verify UI displays all prediction data
- [ ] 7. Test offline behavior (error handling)
- [ ] 8. Test network timeout handling
- [ ] 9. Verify proper error messages to user
- [ ] 10. Test with real kitchen scenarios

---

## Production Deployment Checklist

- [ ] Change `BASE_URL` to production server IP
- [ ] Enable SSL/TLS (use https://)
- [ ] Add API key authentication if required
- [ ] Configure ProGuard/R8 rules
- [ ] Add crash reporting (Firebase Crashlytics)
- [ ] Set up analytics tracking
- [ ] Test on multiple device types
- [ ] Performance test with slow networks
- [ ] Security audit of data transmission
- [ ] User acceptance testing (UAT)

---

## Troubleshooting

### Common Issues:

**1. Connection Refused Error:**
- Ensure server is running at specified IP
- Check firewall settings
- Verify port 8000 is open
- Use device's local network IP (not localhost)

**2. CORS Errors:**
- Backend must have CORS middleware enabled
- Check `app.add_middleware(CORSMiddleware, ...)` in FastAPI

**3. Timeout Errors:**
- Increase timeout values in ApiClient
- Optimize ML model loading time
- Consider background processing for heavy computations

**4. Network Permissions:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

**Documentation Version**: 3.0  
**Last Updated**: September 16, 2026  
**KitchenGuard CSM - Android Mobile Integration Ready**
