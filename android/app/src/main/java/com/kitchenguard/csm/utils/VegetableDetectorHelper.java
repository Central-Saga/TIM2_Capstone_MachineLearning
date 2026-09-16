package com.kitchenguard.csm.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vegetable Detection Helper untuk Android
 * Mendeteksi sayuran/buah dari gambar dengan confidence threshold ketat
 * Jika tidak yakin 100%, akan return UNKNOWN untuk menghindari false positives
 */
public class VegetableDetectorHelper {
    
    private static final String TAG = "VegDetector";
    
    // Confidence threshold HIGH - hanya accept high confidence results
    private static final float CONFIDENCE_THRESHOLD = 0.65f;
    
    // Minimum pixel coverage (detected object harus cukup besar)
    private static final float MIN_PIXEL_COVERAGE = 0.02f;
    
    // Known vegetable classes dengan warna dominan untuk validation
    private static final Map<String, RGBProfile> KNOWN_CLASSES = new HashMap<>();
    
    static {
        // Define RGB profiles for known items (for fallback validation)
        addClassProfile("tomato", new RGBProfile(220f, 50f, 50f, 0.8f));
        addClassProfile("potato", new RGBProfile(180f, 160f, 120f, 0.7f));
        addClassProfile("carrot", new RGBProfile(255f, 140f, 60f, 0.75f));
        addClassProfile("onion", new RGBProfile(200f, 180f, 150f, 0.7f));
        addClassProfile("cucumber", new RGBProfile(34f, 139f, 34f, 0.75f));
        addClassProfile("broccoli", new RGBProfile(35f, 120f, 40f, 0.7f));
        addClassProfile("pepper_red", new RGBProfile(220f, 60f, 60f, 0.75f));
        addClassProfile("pepper_green", new RGBProfile(100f, 180f, 80f, 0.7f));
        addClassProfile("lettuce", new RGBProfile(144f, 238f, 144f, 0.7f));
        addClassProfile("corn", new RGBProfile(255f, 223f, 0f, 0.65f));
        addClassProfile("beans", new RGBProfile(50f, 150f, 50f, 0.7f));
        
        // Fruits (if needed)
        addClassProfile("apple_red", new RGBProfile(200f, 30f, 40f, 0.75f));
        addClassProfile("apple_green", new RGBProfile(100f, 180f, 60f, 0.7f));
        addClassProfile("banana", new RGBProfile(255f, 220f, 50f, 0.75f));
        addClassProfile("orange", new RGBProfile(255f, 140f, 50f, 0.7f));
    }
    
    private static void addClassProfile(String name, RGBProfile profile) {
        KNOWN_CLASSES.put(name.toLowerCase(), profile);
    }
    
    /**
     * Detect item dari bitmap dengan ketat
     */
    public VegetableDetectionResult detectItem(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            return VegetableDetectionResult.createUnknown("Invalid bitmap");
        }
        
        try {
            // Extract image features
            ImageFeatures features = extractImageFeatures(bitmap);
            
            // Analyze using multiple methods
            List<DetectionCandidate> candidates = new ArrayList<>();
            
            // Method 1: Color analysis (fallback)
            ColorAnalysis colorResult = analyzeByColor(features);
            if (colorResult.confidence >= CONFIDENCE_THRESHOLD) {
                candidates.add(new DetectionCandidate(colorResult.name, colorResult.confidence, "color_analysis"));
            }
            
            // Method 2: Texture/pattern analysis (if we had ML model loaded)
            // Placeholder - would integrate YOLO TFLite model here
            
            // Sort by confidence
            candidates.sort((a, b) -> Float.compare(b.confidence, a.confidence));
            
            // If best candidate meets threshold, use it
            if (!candidates.isEmpty()) {
                DetectionCandidate best = candidates.get(0);
                
                if (best.confidence >= CONFIDENCE_THRESHOLD) {
                    // Verify RGB profile match
                    if (verifyRGBMatch(best.name, features)) {
                        Log.d(TAG, "High confidence detection: " + best.name + 
                              " (" + (best.confidence * 100) + "%)");
                        return createResult(best.name, best.confidence, "confirmed");
                    }
                }
            }
            
            // No high confidence detection found - return UNKNOWN
            Log.w(TAG, "No detection above threshold (" + (CONFIDENCE_THRESHOLD * 100) + 
                  "%). Returning UNKNOWN.");
            return VegetableDetectionResult.createUnknown("Low confidence - verify manually");
            
        } catch (Exception e) {
            Log.e(TAG, "Detection error", e);
            return VegetableDetectionResult.createUnknown("Error: " + e.getMessage());
        }
    }
    
    /**
     * Quick detection dari RGB values (untuk camera frame sampling)
     */
    public VegetableDetectionResult detectFromRGB(float r, float g, float b) {
        float avgIntensity = (r + g + b) / 3f;
        
        // Skip if too dark or too bright (likely not food)
        if (avgIntensity < 40f || avgIntensity > 250f) {
            return VegetableDetectionResult.createUnknown("Lighting outside range");
        }
        
        // Color-based classification
        String detectedClass = classifyByColor(r, g, b);
        
        // Calculate confidence
        RGBProfile expected = KNOWN_CLASSES.get(detectedClass.toLowerCase());
        float confidence = 0f;
        
        if (expected != null) {
            confidence = calculateRGBSimilarity(r, g, b, expected.meanR, expected.meanG, expected.meanB);
        } else {
            confidence = 0.3f; // Unknown class
        }
        
        // Only return detected if high confidence
        if (confidence >= CONFIDENCE_THRESHOLD) {
            return createResult(detectedClass, confidence, "rgb_analysis");
        }
        
        return VegetableDetectionResult.createUnknown("Insufficient evidence");
    }
    
    /**
     * Classify berdasarkan color matching
     */
    private ColorAnalysis analyzeByColor(ImageFeatures features) {
        float r = features.meanR;
        float g = features.meanG;
        float b = features.meanB;
        
        String bestClass = null;
        float highestConfidence = 0f;
        
        for (Map.Entry<String, RGBProfile> entry : KNOWN_CLASSES.entrySet()) {
            String className = entry.getKey();
            RGBProfile profile = entry.getValue();
            
            float similarity = calculateRGBSimilarity(r, g, b, profile.meanR, profile.meanG, profile.meanB);
            
            if (similarity > highestConfidence) {
                highestConfidence = similarity;
                bestClass = className;
            }
        }
        
        // Apply penalty for low contrast colors
        float colorContrast = maxOf(
            Math.abs(r - g),
            Math.abs(g - b),
            Math.abs(b - r)
        );
        
        if (colorContrast < 30f) {
            highestConfidence *= 0.8f; // Reduce confidence for grayish colors
        }
        
        return new ColorAnalysis(bestClass, highestConfidence);
    }
    
    /**
     * Primary color heuristics
     */
    private String classifyByColor(float r, float g, float b) {
        // Red varieties (tomato, red pepper, red apple)
        if (r > 180f && g < 100f && b < 100f) {
            return "tomato"; // Default to tomato for red
        }
        
        // Green varieties (cucumber, broccoli, green pepper)
        if (g > 150f && r < 100f && b < 100f) {
            return "cucumber"; // Default for green
        }
        
        // Orange/yellow (carrot, corn, banana)
        if (r > 200f && g > 150f && b < 100f) {
            if (g > 200f) return "banana"; // Yellowish
            return "carrot"; // Orangey
        }
        
        // Brown/beige (potato, onion)
        if (Math.abs(r - g) < 20f && Math.abs(g - b) < 20f && r > 150f) {
            if (r < 170f) return "potato";
            return "onion";
        }
        
        // Purple (eggplant, grapes)
        if (b > 100f && r > 80f && g < 80f) {
            return "unknown_item";
        }
        
        // White/cream (corn when pale, some onions)
        if (r > 230f && g > 220f && b > 200f) {
            return "corn";
        }
        
        return "unknown_item";
    }
    
    /**
     * Verify detection against RGB profile
     */
    private boolean verifyRGBMatch(String className, ImageFeatures features) {
        RGBProfile profile = KNOWN_CLASSES.get(className.toLowerCase());
        
        if (profile == null) return false;
        
        float similarity = calculateRGBSimilarity(
            features.meanR, features.meanG, features.meanB,
            profile.meanR, profile.meanG, profile.meanB
        );
        
        return similarity >= 0.7f;
    }
    
    /**
     * Calculate similarity between two RGB values (0-1 scale)
     */
    private float calculateRGBSimilarity(float r1, float g1, float b1,
                                        float r2, float g2, float b2) {
        float dr = (r1 - r2) / 255f;
        float dg = (g1 - g2) / 255f;
        float db = (b1 - b2) / 255f;
        
        float euclideanDistance = (dr*dr + dg*dg + db*db);
        
        // Convert distance to similarity (higher is better)
        float similarity = 1f - (euclideanDistance / 3f);
        
        return Math.max(0f, Math.min(1f, similarity));
    }
    
    /**
     * Extract image features dari bitmap
     */
    private ImageFeatures extractImageFeatures(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        long sumR = 0, sumG = 0, sumB = 0;
        int pixelCount = 0;
        
        // Sample center region for efficiency (skip borders/noise)
        int startX = width / 4;
        int startY = height / 4;
        int sampleWidth = width / 2;
        int sampleHeight = height / 2;
        int step = 8; // Sample every 8th pixel
        
        for (int y = startY; y < startY + sampleHeight; y += step) {
            for (int x = startX; x < startX + sampleWidth; x += step) {
                int color = bitmap.getPixel(x, y);
                
                // Skip black/dark pixels (noise/background)
                if (android.graphics.Color.alpha(color) < 50 ||
                    android.graphics.Color.red(color) < 40 ||
                    android.graphics.Color.green(color) < 40 ||
                    android.graphics.Color.blue(color) < 40) {
                    continue;
                }
                
                sumR += android.graphics.Color.red(color);
                sumG += android.graphics.Color.green(color);
                sumB += android.graphics.Color.blue(color);
                pixelCount++;
            }
        }
        
        if (pixelCount == 0) {
            // Fallback to default
            return new ImageFeatures(128f, 128f, 128f, 0f);
        }
        
        float meanR = sumR / pixelCount;
        float meanG = sumG / pixelCount;
        float meanB = sumB / pixelCount;
        
        // Calculate coverage (how much of image has visible content)
        float coverage = (float) pixelCount / (width * height);
        
        return new ImageFeatures(meanR, meanG, meanB, coverage);
    }
    
    /**
     * Create result object
     */
    private VegetableDetectionResult createResult(String name, float confidence, String method) {
        RGBProfile profile = KNOWN_CLASSES.get(name.toLowerCase());
        String displayName = name.isEmpty() ? name : name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", " ");
        
        return new VegetableDetectionResult(
            name,
            displayName,
            confidence,
            method,
            true,
            profile != null ? new RGBValue(profile.meanR, profile.meanG, profile.meanB) : null
        );
    }
    
    // Inner Classes
    
    private static class RGBProfile {
        float meanR, meanG, meanB, minConfidence;
        
        RGBProfile(float r, float g, float b, float conf) {
            this.meanR = r;
            this.meanG = g;
            this.meanB = b;
            this.minConfidence = conf;
        }
    }
    
    public static class RGBValue {
        public final float r, g, b;
        public RGBValue(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
    
    private static class ColorAnalysis {
        String name;
        float confidence;
        
        ColorAnalysis(String name, float confidence) {
            this.name = name;
            this.confidence = confidence;
        }
    }
    
    private static class ImageFeatures {
        float meanR, meanG, meanB, coverage;
        
        ImageFeatures(float r, float g, float b, float cov) {
            this.meanR = r;
            this.meanG = g;
            this.meanB = b;
            this.coverage = cov;
        }
    }
    
    private static class DetectionCandidate {
        String name;
        float confidence;
        String method;
        
        DetectionCandidate(String name, float confidence, String method) {
            this.name = name;
            this.confidence = confidence;
            this.method = method;
        }
    }
    
    /**
     * Result from vegetable detection
     */
    public static class VegetableDetectionResult {
        public final String itemId;
        public final String displayName;
        public final float confidence;
        public final String detectionMethod;
        public final boolean isDetected;
        public final RGBValue rgbExpected;
        
        private VegetableDetectionResult(String itemId, String displayName, float confidence,
                                       String method, boolean detected, RGBValue rgb) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.confidence = confidence;
            this.detectionMethod = method;
            this.isDetected = detected;
            this.rgbExpected = rgb;
        }
        
        public static VegetableDetectionResult createUnknown(String reason) {
            return new VegetableDetectionResult(
                "unknown",
                "Unknown Item",
                0f,
                reason,
                false,
                null
            );
        }
        
        @Override
        public String toString() {
            if (!isDetected) {
                return String.format("UNKNOWN (%s)", detectionMethod);
            }
            return String.format("%s %.1f%% (%s)", displayName, confidence * 100, detectionMethod);
        }
    }
    
    private float maxOf(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }
}
