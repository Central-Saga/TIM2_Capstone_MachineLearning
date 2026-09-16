package com.kitchenguard.csm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.kitchenguard.csm.databinding.ActivityCameraScannerBinding
import com.kitchenguard.csm.utils.VegetableDetectorHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Enhanced Camera Scanner Activity - Strict Detection dengan Unknown Fallback
 * Modern Material Design 3 UI + Reliable vegetable detection
 */
class CameraScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    
    private var camera: Camera? = null
    private lateinit var vegetableDetector: VegetableDetectorHelper
    
    private val orientationListener: OrientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {}
        }
    }
    
    companion object {
        private const val TAG = "CameraScanner"
        private const val PERMISSION_REQUEST_CODE = 2001
        
        // STRICT confidence threshold - minimal 70% baru accept
        private const val CONFIDENCE_THRESHOLD = 0.70f
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupModernUI()
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        checkPermissionsAndStartCamera()
    }
    
    /**
     * Setup Modern Material Design 3 UI
     */
    private fun setupModernUI() {
        applyRoundedCorners()
        setupAnimations()
        
        try {
            vegetableDetector = VegetableDetectorHelper()
            Log.d(TAG, "Vegetable Detector initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize detector", e)
        }
        
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCapture.setOnClickListener { captureAndAnalyze() }
        binding.btnFlashlight.setOnClickListener { toggleFlashlight() }
        
        binding.tvStatus.text = "📸 Point camera at vegetables/fruits..."
        binding.tvStatus.setTextColor(getColor(R.color.md_theme_secondary))
    }
    
    private fun applyRoundedCorners() {
        binding.cardResult.radius = 24f
        binding.cardResult.elevation = 16f
    }
    
    private fun setupAnimations() {
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator()).start()
        
        binding.cardResult.scaleX = 0.8f
        binding.cardResult.scaleY = 0.8f
        binding.cardResult.animate()
            .scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(200)
            .setInterpolator(FastOutSlowInInterpolator()).start()
    }
    
    private fun checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processFrame(imageProxy) }
                }
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
                orientationListener.enable()
                Log.d(TAG, "Camera started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera", e)
                runOnUiThread {
                    Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Process frame dengan strict detection
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val width = imageProxy.width / 2
            val height = imageProxy.height / 2
            val centerX = imageProxy.width / 2
            val centerY = imageProxy.height / 2
            
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f
            var count = 0
            
            val step = 10
            
            for (y in (centerY - height / 2) until (centerY + height / 2) step step) {
                for (x in (centerX - width / 2) until (centerX + width / 2) step step) {
                    if (y in 0 until imageProxy.height && x in 0 until imageProxy.width) {
                        val idx = y * imageProxy.width + x
                        if (idx in 0 until bytes.size) {
                            val b = bytes[idx].toInt() and 0xFF
                            val g = if (idx + 1 < bytes.size) bytes[idx + 1].toInt() and 0xFF else b
                            val r = if (idx + 2 < bytes.size) bytes[idx + 2].toInt() and 0xFF else b
                            
                            if (b in 51..239 && g in 51..239 && r in 51..239) {
                                sumR += r
                                sumG += g
                                sumB += b
                                count++
                            }
                        }
                    }
                }
            }
            
            if (count > 0) {
                val meanR = sumR / count
                val meanG = sumG / count
                val meanB = sumB / count
                
                analyzeWithStrictDetection(meanR, meanG, meanB)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Analyze dengan STRICT mode - UNKNOWN jika confidence rendah
     */
    private fun analyzeWithStrictDetection(r: Float, g: Float, b: Float) {
        try {
            val result = vegetableDetector.detectFromRGB(r, g, b)
            
            Log.v(TAG, "Raw detection: ${result.displayName} (${result.confidence * 100}%)")
            
            if (!result.isDetected || result.confidence < CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "Rejected low confidence: ${result.confidence * 100}%")
                runOnUiThread {
                    showUnknownState()
                }
                return
            }
            
            Log.d(TAG, "Accepted high confidence detection")
            runOnUiThread {
                updateDetectionUI(result)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Analysis error", e)
            runOnUiThread {
                showUnknownState()
            }
        }
    }
    
    /**
     * Show UNKNOWN state ketika tidak yakin
     */
    private fun showUnknownState() {
        binding.progressBar.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE
        
        binding.tvCategory.text = "UNKNOWN"
        binding.tvCategory.setTextColor(getColor(R.color.grey_600))
        
        binding.tvIngredient.text = "Unknown Item"
        binding.tvIngredient.setTextColor(getColor(R.color.md_theme_on_surface))
        
        binding.tvRgb.text = "--"
        binding.tvConfidence.text = "--"
        binding.confidenceBar.progress = 0
        
        binding.tvFreshness.text = "N/A"
        binding.cardFreshness.setCardBackgroundColor(getColor(R.color.grey_400))
        
        binding.tvStatus.text = "⚠ Unable to identify item\nPoint more closely or improve lighting"
        binding.tvStatus.setTextColor(getColor(R.color.orange_600))
        
        binding.tvRecommendation.visibility = View.GONE
    }
    
    /**
     * Update UI dengan hasil deteksi yang valid
     */
    private fun updateDetectionUI(result: VegetableDetectorHelper.VegetableDetectionResult) {
        binding.progressBar.visibility = View.GONE
        binding.cardResult.visibility = View.VISIBLE
        
        binding.tvCategory.text = result.itemId.uppercase()
        binding.tvCategory.setTextColor(getCategoryColor(result.itemId))
        
        binding.tvIngredient.text = result.displayName
        binding.tvIngredient.translationX = 100f
        binding.tvIngredient.animate()
            .translationX(0f).setDuration(300).setInterpolator(FastOutSlowInInterpolator()).start()
        
        val rgb = result.rgbExpected ?: VegetableDetectorHelper.RGBValue(128f, 128f, 128f)
        binding.tvRgb.text = String.format("RGB(%d, %d, %d)", rgb.r.toInt(), rgb.g.toInt(), rgb.b.toInt())
        
        val freshness = determineFreshnessByRGB(rgb)
        binding.tvFreshness.text = freshness
        binding.cardFreshness.setCardBackgroundColor(getFreshnessColor(freshness))
        
        val progress = (result.confidence * 100).toInt()
        binding.confidenceBar.progress = progress
        binding.tvConfidence.text = "${String.format("%.1f", result.confidence * 100)}%"
        
        binding.tvStatus.text = "✓ Detected: ${result.displayName}"
        binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        
        binding.tvRecommendation.visibility = View.GONE
    }
    
    private fun determineFreshnessByRGB(rgb: VegetableDetectorHelper.RGBValue): String {
        val avg = (rgb.r + rgb.g + rgb.b) / 3f
        return when {
            avg > 200f -> "FRESH"
            avg > 160f -> "GOOD"
            else -> "NEEDS REVIEW"
        }
    }
    
    private fun getCategoryColor(categoryId: String): Int {
        return when (categoryId.lowercase()) {
            "tomato", "pepper_red", "apple_red" -> getColor(R.color.red_500)
            "cucumber", "broccoli", "beans" -> getColor(R.color.green_600)
            "carrot", "corn", "banana" -> getColor(R.color.orange_500)
            "potato", "onion" -> getColor(R.color.brown_600)
            "unknown" -> getColor(R.color.grey_600)
            else -> getColor(R.color.md_theme_primary)
        }
    }
    
    private fun getFreshnessColor(freshness: String): Int {
        return when (freshness.uppercase()) {
            "FRESH" -> getColor(android.R.color.holo_green_dark)
            "GOOD" -> getColor(R.color.green_500)
            "NEEDS REVIEW" -> getColor(R.color.yellow_600)
            else -> getColor(R.color.grey_400)
        }
    }
    
    private fun captureAndAnalyze() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        
        cameraExecutor.execute {
            Thread.sleep(300)
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.btnCapture.isEnabled = true
            }
        }
    }
    
    private fun toggleFlashlight() {
        try {
            val torchState = camera?.cameraInfo?.torchState?.value
            val isCurrentlyOn = torchState == TorchState.ON
            camera?.cameraControl?.enableTorch(!isCurrentlyOn)
            binding.btnFlashlight.alpha = if (!isCurrentlyOn) 1f else 0.5f
        } catch (e: Exception) {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        orientationListener.disable()
    }
}
