# 🎤 KitchenGuard CSM - Voice Input Implementation (Complete)

**Version**: 3.0  
**Feature**: Speech-to-Text for Hands-Free Waste Recording  
**Status**: Ready to Implement  

---

## 🎯 Use Case

Kitchen staff can record waste descriptions using voice instead of typing:
- Dirty/hands-free environment (cooking, cleaning)
- Faster data entry during busy service periods
- Reduced errors from manual typing
- Natural language input matching how staff describe problems

---

## Phase 2.1: AndroidManifest Permissions

### File: `app/src/main/AndroidManifest.xml`

Add these permissions inside `<manifest>` tag:

```xml
<!-- Audio Recording Permission -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Network Access (for API calls) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Optional: Foreground Service for continuous listening -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Also add capability declaration:

```xml
<uses-feature android:name="android.hardware.microphone" android:required="false" />
```

---

## Phase 2.2: Layout XML

### File: `app/src/main/res/layout/activity_speech_input.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F5F5F5">

    <!-- Header -->
    <TextView
        android:id="@+id/tvHeader"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Record Waste Description"
        android:textSize="24sp"
        android:textStyle="bold"
        android:textColor="#333333"
        android:padding="16dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Transcription Display Area -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardTranscription"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_margin="16dp"
        app:cardCornerRadius="12dp"
        app:cardElevation="4dp"
        app:layout_constraintTop_toBottomOf="@id/tvHeader"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/btnRecord">

        <ScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <!-- Status Indicator -->
                <TextView
                    android:id="@+id/tvRecordingStatus"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Tap microphone button to start recording"
                    android:textSize="14sp"
                    android:textColor="#757575"
                    android:gravity="center"
                    android:layout_marginBottom="12dp"/>

                <!-- Transcribed Text -->
                <TextView
                    android:id="@+id/tvTranscription"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text=""
                    android:textSize="18sp"
                    android:textColor="#212121"
                    android:minHeight="150dp"
                    android:padding="12dp"
                    android:background="@drawable/text_background"
                    android:lineSpacingExtra="4dp"/>

                <!-- Clear Button -->
                <Button
                    android:id="@+id/btnClear"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Clear Text"
                    android:layout_marginTop="8dp"
                    style="@style/Widget.MaterialComponents.Button.OutlinedButton"/>

            </LinearLayout>
        </ScrollView>
    </com.google.android.material.card.MaterialCardView>

    <!-- Record Button -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/btnRecord"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:layout_marginBottom="32dp"
        android:src="@android:drawable/ic_btn_speak_now"
        android:contentDescription="Record Voice"
        app:tint="#FFFFFF"
        app:layout_constraintBottom_toTopOf="@id/btnAnalyze"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <!-- Analyze Button -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnAnalyze"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Analyze Waste"
        android:enabled="false"
        android:textSize="16sp"
        android:textAllCaps="false"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginHorizontal="32dp"
        android:layout_marginBottom="16dp"/>

    <!-- Processing Indicator -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toBottomOf="@id/cardTranscription"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="24dp"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### File: `app/src/main/res/drawable/text_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFFFFF"/>
    <corners android:radius="8dp"/>
    <stroke
        android:width="1dp"
        android:color="#E0E0E0"/>
</shape>
```

---

## Phase 2.3: Speech Recognition Activity

### File: `app/src/main/java/com/kitchenguard/ui/SpeechInputActivity.kt`

```kotlin
package com.kitchenguard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.kitchenguard.databinding.ActivitySpeechInputBinding
import com.kitchenguard.model.PredictionData
import com.kitchenguard.repository.WasteRepository
import com.kitchenguard.utils.ApiClient
import com.kitchenguard.viewmodel.WasteClassifierViewModel
import kotlinx.coroutines.launch

class SpeechInputActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySpeechInputBinding
    private lateinit var viewModel: WasteClassifierViewModel
    private var isListening = false
    
    // Speech recognizer components
    private var speechRecognizer: SpeechRecognizer? = null
    private var hasSpeechService = true
    
    // Request codes for permissions
    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
        private const val SPEECH_RESULT_REQUEST_CODE = 202
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeechInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initializeApi()
        setupViewModel()
        checkSpeechAvailability()
        setupRecognitionListener()
        setupClickListeners()
        
        updateUIForRecordingState()
    }
    
    private fun initializeApi() {
        ApiClient.initialize(applicationContext)
        val repository = WasteRepository(ApiClient.getService())
        viewModel = ViewModelProvider(this)[WasteClassifierViewModel::class.java]
    }
    
    private fun setupViewModel() {
        viewModel.classificationResult.observe(this) { result ->
            handleClassificationResult(result)
        }
    }
    
    private fun checkSpeechAvailability() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            hasSpeechService = false
            showToast("Voice recognition not available on this device")
            binding.btnRecord.isEnabled = false
            binding.tvRecordingStatus.text = "Voice recognition unavailable"
            return
        }
    }
    
    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            
            override fun onReadyForSpeech(params: Bundle) {
                runOnUiThread {
                    binding.tvRecordingStatus.text = "Listening... Speak now"
                    isListening = true
                    updateUIForRecordingState()
                }
            }
            
            override fun onBeginningOfSpeech() {
                // Visual feedback that audio is being captured
                runOnUiThread {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could show waveform or volume indicator
            }
            
            override fun onBufferReceived(buffer: ByteArray) {
                // Partial results received
            }
            
            override fun onEndOfSpeech() {
                runOnUiThread {
                    binding.tvRecordingStatus.text = "Processing speech..."
                    isListening = false
                    updateUIForRecordingState()
                }
            }
            
            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                runOnUiThread {
                    binding.tvRecordingStatus.text = "Error: $errorMessage"
                    isListening = false
                    updateUIForRecordingState()
                    showToast(errorMessage)
                }
            }
            
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { recognizedText ->
                    runOnUiThread {
                        binding.tvTranscription.text = recognizedText
                        
                        // Enable analyze button when text is available
                        binding.btnAnalyze.isEnabled = true
                        binding.btnAnalyze.text = "Analyze: '${recognizedText.take(30)}...'"
                    }
                    
                    // Auto-analyze after successful transcription
                    autoAnalyzeText(recognizedText)
                }
            }
            
            override fun onPartialResults(partialResults: Bundle) {
                val partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partialMatches?.firstOrNull()?.let { partialText ->
                    runOnUiThread {
                        binding.tvTranscription.text = partialText
                        binding.btnAnalyze.isEnabled = partialText.isNotBlank()
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle) {
                // Handle events if needed
            }
        })
    }
    
    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error (code: $error)"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnRecord.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
        
        binding.btnAnalyze.setOnClickListener {
            val text = binding.tvTranscription.text.toString()
            if (text.isNotBlank()) {
                viewModel.classifyWaste(text)
            }
        }
        
        binding.btnClear.setOnClickListener {
            binding.tvTranscription.text = ""
            binding.btnAnalyze.isEnabled = false
            binding.btnAnalyze.text = "Analyze Waste"
        }
    }
    
    private fun startListening() {
        if (!hasSpeechService) {
            showToast("Voice recognition not available")
            return
        }
        
        // Request audio permission
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }
        
        // Create intent for speech recognition
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID") // Indonesian language
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) // Get top 3 possibilities
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            showToast("Failed to start voice recognition: ${e.message}")
        }
    }
    
    private fun stopListening() {
        speechRecognizer?.stopListening()
        updateUIForRecordingState()
    }
    
    private fun updateUIForRecordingState() {
        if (isListening) {
            binding.btnRecord.setImageResource(android.R.drawable.ic_btn_stop)
            binding.btnRecord.contentDescription = "Stop Recording"
            binding.tvRecordingStatus.setTextColor(ContextCompat.getColor(this, R.color.recording))
        } else {
            binding.btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now)
            binding.btnRecord.contentDescription = "Record Voice"
            binding.tvRecordingStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }
    
    private fun autoAnalyzeText(text: String) {
        // Small delay to let user see the transcribed text
        lifecycleScope.launch {
            delay(500)
            viewModel.classifyWaste(text)
        }
    }
    
    private fun handleClassificationResult(result: Result<PredictionData>) {
        result.onSuccess { prediction ->
            showAnalysisResults(prediction)
        }.onFailure { error ->
            showToast("Analysis failed: ${error.message}")
        }
    }
    
    private fun showAnalysisResults(prediction: PredictionData) {
        // Navigate to classification result screen or show dialog
        val resultIntent = Intent(this, ClassificationResultActivity::class.java).apply {
            putExtra("predicted_class", prediction.predictedClass)
            putExtra("confidence", prediction.confidence.toFloat())
            putExtra("gate_status", prediction.gateStatus)
            putExtra("action_recommendation", prediction.actionRecommendation)
            
            prediction.financialImpact?.let { loss ->
                putExtra("total_loss_rupiah", loss.totalLossRupiah.toLong())
                putExtra("priority_level", loss.priorityLevel)
            }
        }
        
        startActivity(resultIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startListening()
                } else {
                    showToast("Microphone permission required for voice input")
                    binding.btnRecord.isEnabled = false
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
```

---

## Phase 2.4: Backend Preprocessing Endpoint

The FastAPI server already includes the voice preprocessing endpoint at `/api/voice/transcribe`:

```python
@app.post("/api/voice/transcribe", tags=["Voice Input"])
def voice_transcription(req: dict):
    """
    Preprocess voice input text for ML classification
    
    Request Body:
    {
        "transcribed_text": "string (from Android speech recognition)",
        "language": "string (default: 'id')"
    }
    """
```

Usage example:
```bash
curl -X POST http://localhost:8000/api/voice/transcribe \
  -H "Content-Type: application/json" \
  -d '{"transcribed_text": "Daging sapi berbau busuk berlendir"}'
```

Returns preprocessed text ready for ML classification.

---

## Testing Checklist

- [ ] Microphone permission granted
- [ ] Voice recognition service available on device
- [ ] Recording starts when button tapped
- [ ] Real-time transcription updates visible
- [ ] Text displayed accurately after recognition
- [ ] Analysis button becomes enabled with text
- [ ] Automatic classification works
- [ ] Results displayed correctly in result screen
- [ ] Error handling for network issues
- [ ] Stop recording button works properly

---

## Production Considerations

### Performance Optimization:

1. **Use local STT engine** when available (faster than cloud)
2. **Cache frequent phrases** for faster classification
3. **Batch multiple entries** during end-of-shift summary
4. **Offline mode** with queued requests

### Accessibility Features:

1. Support for hearing-impaired staff (visual feedback)
2. Screen reader compatibility (TalkBack support)
3. Large touch targets for dirty hands
4. Haptic feedback on actions

### Privacy & Security:

1. No voice recordings stored
2. Only transcribed text processed
3. Encryption for data transmission
4. Compliance with data protection laws

---

## Alternative Solutions

If native Speech Recognition doesn't work well:

### Option 1: Google Cloud Speech-to-Text
- Higher accuracy (paid service)
- Better multilingual support
- Requires API key integration

### Option 2: Third-party SDK
- ViSTARA Speech Recognition
- Wit.ai by Facebook
- Custom voice models

### Option 3: Text-Based Fallback
- Keep keyboard input option
- Offer voice suggestions
- Quick action buttons for common scenarios

---

**Documentation Version**: 3.0  
**Last Updated**: September 16, 2026  
**KitchenGuard CSM - Voice Input Implementation Complete**
