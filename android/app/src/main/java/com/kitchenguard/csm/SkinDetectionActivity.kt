package com.kitchenguard.csm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.kitchenguard.csm.databinding.ActivitySkinDetectionBinding
import com.kitchenguard.csm.utils.MachineLearningUtils
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Skin Detection Activity
 * Mendeteksi warna kulit (fair skin/orang putih) secara real-time
 * Digunakan untuk hygiene monitoring di dapur
 */
class SkinDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkinDetectionBinding
    private lateinit var cameraExecutor: ExecutorService
    
    private var camera: Camera? = null
    private var currentAnalyzer: ImageAnalysis.Analyzer? = null
    
    companion object {
        private const val TAG = "SkinDetection"
        private const val PERMISSION_REQUEST_CODE = 1001
        
        // Fair skin RGB ranges (dari model training)
        private const val FAIR_SKIN_MIN_R = 200f
        private const val FAIR_SKIN_MIN_G = 180f
        private const val FAIR_SKIN_MIN_B = 160f
        
        // Thresholds untuk klasifikasi (Diselaraskan dengan PRD & Backend Gate: 85%)
        private const val CONFIDENCE_THRESHOLD = 0.85f
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkinDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        checkPermissionsAndStartCamera()
        setupUIListeners()
    }
    
    private fun checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun setupUIListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnDetectSkin.setOnClickListener {
            detectSkinFromCurrentFrame()
        }
        
        binding.btnAnalyzeSample.setOnClickListener {
            analyzePredefinedSample()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImageProxy(imageProxy)
                    }
                }
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera", e)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Detect skin dari current frame
     */
    private fun detectSkinFromCurrentFrame() {
        binding.progressBar.visibility = View.VISIBLE
        binding.resultStatus.text = "Analyzing..."
        
        val frameBitmap = getPreviewFrameAsBitmap()
        cameraExecutor.execute {
            try {
                val result = analyzeBitmapForSkin(frameBitmap)
                frameBitmap?.recycle()
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    displayDetectionResult(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Detection error", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.resultStatus.text = "Error: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Main algorithm untuk deteksi fair skin
     * Return data class dengan hasil deteksi
     */
    private fun analyzeBitmapForSkin(bitmap: Bitmap?): SkinDetectionResult {
        var totalPixels = 0
        var fairSkinPixels = 0
        val detectedRegions = mutableListOf<SkinRegion>()
        
        if (bitmap != null) {
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val color = bitmap.getPixel(x, y)
                    
                    // Skip black/border pixels
                    if (color == Color.BLACK) continue
                    
                    val r = Color.red(color).toFloat()
                    val g = Color.green(color).toFloat()
                    val b = Color.blue(color).toFloat()
                    
                    totalPixels++
                    
                    // Check jika fair skin berdasarkan RGB thresholds
                    if (isFairSkin(r, g, b)) {
                        fairSkinPixels++
                        
                        // Kategorisasi skin type berdasarkan intensity
                        val skinType = classifyFairSkinType(r, g, b)
                        detectedRegions.add(SkinRegion(x, y, skinType))
                    }
                }
            }
        }
        
        // Calculate statistics
        val fairSkinPercentage = if (totalPixels > 0) {
            (fairSkinPixels.toFloat() / totalPixels) * 100
        } else {
            0f
        }
        
        val isFairSkinDetected = fairSkinPercentage > 10 && 
                                  detectedRegions.isNotEmpty()
        
        return SkinDetectionResult(
            isFairSkinDetected = isFairSkinDetected,
            fairSkinPercentage = fairSkinPercentage,
            averageSkinType = detectAverageSkinType(detectedRegions),
            detectedRegionsCount = detectedRegions.size,
            classificationConfidence = calculateConfidence(fairSkinPercentage, detectedRegions.size)
        )
    }
    
    /**
     * Cek apakah pixel ini fair skin
     */
    private fun isFairSkin(r: Float, g: Float, b: Float): Boolean {
        return r >= FAIR_SKIN_MIN_R && 
               g >= FAIR_SKIN_MIN_G && 
               b >= FAIR_SKIN_MIN_B
    }
    
    /**
     * Klasifikasi fair skin type (1-3 berdasarkan Fitzpatrick scale)
     */
    private fun classifyFairSkinType(r: Float, g: Float, b: Float): String {
        val avgIntensity = (r + g + b) / 3
        
        return when {
            avgIntensity > 230f -> "FAIR_1" // Sangat terang
            avgIntensity > 210f -> "FAIR_2" // Terang
            else -> "FAIR_3" // Sedikit tan
        }
    }
    
    /**
     * Average skin type dari semua detected regions
     */
    private fun detectAverageSkinType(regions: List<SkinRegion>): String {
        if (regions.isEmpty()) return "UNKNOWN"
        
        val types = regions.map { it.skinType }
        val fair1Count = types.count { it == "FAIR_1" }
        val fair2Count = types.count { it == "FAIR_2" }
        val fair3Count = types.count { it == "FAIR_3" }
        
        return when {
            fair1Count > fair2Count && fair1Count > fair3Count -> "FAIR_1"
            fair2Count > fair1Count && fair2Count > fair3Count -> "FAIR_2"
            else -> "FAIR_3"
        }
    }
    
    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(fairSkinPercentage: Float, regionCount: Int): Float {
        val percentageScore = kotlin.math.min(fairSkinPercentage / 50f, 1.0f)
        val countScore = kotlin.math.min(regionCount.toFloat() / 1000f, 1.0f)
        
        return (percentageScore + countScore) / 2f
    }
    
    /**
     * Display hasil deteksi ke UI
     */
    private fun displayDetectionResult(result: SkinDetectionResult) {
        val statusColor = if (result.isFairSkinDetected) {
            android.R.color.holo_green_dark
        } else {
            android.R.color.holo_red_dark
        }
        
        binding.resultStatus.setTextColor(getColor(statusColor))
        binding.resultStatus.text = if (result.isFairSkinDetected) {
            "✓ Fair Skin Detected"
        } else {
            "✗ No Fair Skin Detected"
        }
        
        binding.resultDetails.text = """
            | Fair Skin Coverage: %.1f%%
            | Skin Type: ${result.averageSkinType}
            | Regions Detected: ${result.detectedRegionsCount}
            | Confidence: %.1f%%
            | Hygiene Note: ${getHygieneNote(result)}
        """.trimMargin().format(
            result.fairSkinPercentage,
            result.classificationConfidence * 100
        )
        
        // Update badge
        binding.badgeText.text = if (result.isFairSkinDetected) "OK" else "CHECK"
        binding.badgeText.setBackgroundColor(
            getColor(if (result.isFairSkinDetected) android.R.color.holo_green_dark 
                     else android.R.color.holo_red_dark)
        )
    }
    
    /**
     * Get hygiene note based on detection result
     */
    private fun getHygieneNote(result: SkinDetectionResult): String {
        return when {
            !result.isFairSkinDetected -> "No human skin detected - verify area"
            result.averageSkinType == "FAIR_1" -> "Very light skin - ensure gloves are worn"
            result.fairSkinPercentage > 30f -> "High skin exposure - mandatory gloves required"
            else -> "Normal skin exposure observed"
        }
    }
    
    private fun analyzePredefinedSample() {
        binding.progressBar.visibility = View.VISIBLE
        binding.resultStatus.text = "Analyzing sample..."
        
        val testBitmap = generateTestSkinPattern()
        binding.imageView.setImageBitmap(testBitmap)
        
        cameraExecutor.execute {
            try {
                val result = analyzeBitmapForSkin(testBitmap)
                
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    displayDetectionResult(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sample analysis error", e)
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.resultStatus.text = "Error: ${e.message}"
                }
            }
        }
    }
    
    private fun generateTestSkinPattern(): Bitmap {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        
        // Draw fair skin colored oval
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = Color.rgb(240, 220, 200)
        }
        
        canvas.drawOval(40f, 40f, 160f, 160f, paint)
        
        return bitmap
    }
    
    private fun getPreviewFrameAsBitmap(): Bitmap? {
        return try {
            binding.previewView.bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame", e)
            null
        }
    }
    
    private fun analyzeImageProxy(imageProxy: ImageProxy) {
        // This is called for every camera frame
        // Implement efficient frame analysis here
        imageProxy.close()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

/**
 * Data class for skin detection results
 */
data class SkinDetectionResult(
    val isFairSkinDetected: Boolean,
    val fairSkinPercentage: Float,
    val averageSkinType: String,
    val detectedRegionsCount: Int,
    val classificationConfidence: Float
)

/**
 * Data class untuk region yang terdeteksi
 */
data class SkinRegion(
    val x: Int,
    val y: Int,
    val skinType: String
)

/**
 * Enhanced Vision Scanner untuk kamera scanning bahan makanan
 * Support real-time detection dengan confidence scoring
 */
class VisionScanner {
    
    private var mlUtils: MachineLearningUtils? = null
    
    fun initialize(context: Context) {
        try {
            mlUtils = MachineLearningUtils(context)
        } catch (e: Exception) {
            println("Vision Scanner: ML Utils not available")
        }
    }
    
    /**
     * Scan bahan dari RGB values (dari camera frame)
     */
    fun scanIngredientFromRGB(r: Float, g: Float, b: Float): IngredientResult {
        // Fallback detection menggunakan color analysis
        return fallbackDetectIngredient(r, g, b)
    }
    
    /**
     * Quick detection berdasarkan warna dominan
     */
    private fun fallbackDetectIngredient(r: Float, g: Float, b: Float): IngredientResult {
        // Color-based classification
        val category = when {
            r > 180 && g < 100 && b < 100 -> "vegetables"      // RED -> tomato, pepper
            r < 100 && g > 150 && b < 100 -> "vegetables"     // GREEN -> cucumber, broccoli
            r > 200 && g > 150 && b < 100 -> "fruits"         // ORANGE/YELLOW
            r > 150 && g > 50 && b > 50 -> "meat"             // RED/PINK meat
            r > 200 && g > 200 && b > 180 -> "dairy"          // WHITE/CREAM
            else -> "vegetables"                               // Default
        }
        
        val ingredient = when {
            category == "vegetables" && r > 200 && g < 80 -> "tomato"
            category == "vegetables" && r < 100 && g > 150 -> "cucumber"
            category == "vegetables" && r > 200 && g > 100 -> "pepper_red"
            category == "fruits" && r > 220 && g < 100 -> "apple_red"
            category == "fruits" && r > 150 && g > 150 -> "banana"
            category == "meat" && r > 180 && g < 60 -> "beef"
            category == "meat" && r > 220 && g > 180 -> "chicken"
            else -> "unknown"
        }
        
        val confidence = calculateConfidence(r, g, b)
        
        return IngredientResult(
            category = category,
            ingredient = ingredient,
            rgb = Triple(r, g, b),
            confidence = confidence,
            freshness = determineFreshness(r, g, b),
            recommendations = generateRecommendations(ingredient)
        )
    }
    
    private fun calculateConfidence(r: Float, g: Float, b: Float): Float {
        // Simple heuristic based on color distinctiveness
        val colorDistinction = maxOf(
            abs(r - g),
            abs(g - b),
            abs(b - r)
        ) / 255f
        
        val intensity = (r + g + b) / 3f / 255f
        
        return minOf((colorDistinction + intensity) / 2f + 0.4f, 0.95f)
    }
    
    private fun determineFreshness(r: Float, g: Float, b: Float): String {
        // Heuristic based on RGB characteristics
        val avg = (r + g + b) / 3f
        
        return when {
            avg > 220 -> "FRESH"
            avg > 180 -> "GOOD"
            avg > 140 -> "FAIR"
            else -> "NEEDS REVIEW"
        }
    }
    
    private fun generateRecommendations(ingredient: String): List<String> {
        val recommendations = mutableMapOf<String, List<String>>()
        
        recommendations["tomato"] = listOf(
            "Check for bruises or soft spots",
            "Store at room temperature until ripe",
            "Refrigerate if overripe"
        )
        
        recommendations["potato"] = listOf(
            "Store in cool, dark place",
            "Avoid refrigeration (converts starch to sugar)"
        )
        
        recommendations["apple_red"] = listOf(
            "Store in refrigerator for crispiness",
            "Keep away from other produce (ethylene gas)"
        )
        
        recommendations["unknown"] = listOf(
            "Inspect visually before use",
            "Smell check for spoilage"
        )
        
        return recommendations.getOrDefault(ingredient, listOf("Use within 24 hours"))
    }
}

data class IngredientResult(
    val category: String,
    val ingredient: String,
    val rgb: Triple<Float, Float, Float>,
    val confidence: Float,
    val freshness: String,
    val recommendations: List<String>
) {
    override fun toString(): String {
        return """
        |Category: $category
        |Ingredient: $ingredient
        |RGB: (${rgb.first.toInt()}, ${rgb.second.toInt()}, ${rgb.third.toInt()})
        |Confidence: ${String.format("%.1f", confidence * 100)}%
        |Freshness: $freshness
        |Recommendations:
        |${recommendations.joinToString("\n|   - ")}
    """.trimMargin()
    }
}
