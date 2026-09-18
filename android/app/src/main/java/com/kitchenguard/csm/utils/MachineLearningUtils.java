package com.kitchenguard.csm.utils;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Machine Learning Utilities untuk KitchenGuard CSM
 * Menggabungkan Skin Detection dan Waste Classification dalam satu helper
 */
public class MachineLearningUtils {
    
    private Interpreter skinDetector;
    private Interpreter wasteClassifier;
    
    private List<String> categoryClasses;
    private Map<Integer, String> indexToCategory;
    private List<String> skinTypeClasses;
    
    private static final int[] CATEGORY_CLASSES = {0, 1}; // 0: FACE, 1: HAND (matches android_encoding_map.json)
    
    public MachineLearningUtils(Context context) throws IOException {
        loadModels(context);
    }
    
    /**
     * Load TFLite models dari assets folder
     */
    private void loadModels(Context context) throws IOException {
        // Load skin detector model
        try {
            ByteBuffer skinBuffer = FileUtil.loadMappedFile(
                context, "skin_model.tflite"
            );
            skinDetector = new Interpreter(skinBuffer);
            System.out.println("Skin detector model loaded successfully");
        } catch (Exception e) {
            System.err.println("Skin detector not available, using fallback");
            skinDetector = null;
        }
        
        // Setup category classes mapping (aligned with android_encoding_map.json)
        categoryClasses = new ArrayList<>();
        categoryClasses.add("FACE");
        categoryClasses.add("HAND");
        indexToCategory = new HashMap<>();
        for (int i = 0; i < categoryClasses.size(); i++) {
            indexToCategory.put(i, categoryClasses.get(i));
        }
        
        // Load waste classifier jika ada
        try {
            ByteBuffer wasteBuffer = FileUtil.loadMappedFile(
                context, "waste_classifier.tflite"
            );
            wasteClassifier = new Interpreter(wasteBuffer);
            System.out.println("Waste classifier model loaded successfully");
        } catch (Exception e) {
            System.err.println("Waste classifier not available, using keyword matching");
            wasteClassifier = null;
        }
    }
    
    /**
     * Analyze skin from RGB values (fallback tanpa TFLite)
     * Menggunakan logic sederhana berbasis threshold
     */
    public SkinDetectionResult detectSkinSimple(float r, float g, float b) {
        // Fair skin thresholds dari training data
        boolean isFairSkin = r >= 200f && g >= 180f && b >= 160f;
        
        if (!isFairSkin) {
            return createNegativeResult(0.95f);
        }
        
        // Calculate average intensity untuk determine skin type
        float avgIntensity = (r + g + b) / 3f;
        
        String skinType;
        float confidence;
        
        if (avgIntensity > 230f) {
            skinType = "FAIR_1";
            confidence = 0.85f + (avgIntensity - 230f) / 25f * 0.1f;
        } else if (avgIntensity > 210f) {
            skinType = "FAIR_2";
            confidence = 0.80f + (avgIntensity - 210f) / 20f * 0.1f;
        } else {
            skinType = "FAIR_3";
            confidence = 0.75f + (avgIntensity - 200f) / 10f * 0.1f;
        }
        
        confidence = Math.min(confidence, 0.98f);
        
        return new SkinDetectionResult(
            true,
            skinType,
            confidence,
            getHygieneNoteForSkinType(skinType),
            r, g, b,
            avgIntensity
        );
    }
    
    /**
     * Main API untuk skin detection dengan multiple pixels
     */
    public SkinDetectionResult analyzeSkinMap(List<Float[]> pixelValues) {
        List<SkinDetectionResult> results = new ArrayList<>();
        
        for (Float[] pixel : pixelValues) {
            if (pixel.length != 3) continue;
            
            SkinDetectionResult result = detectSkinSimple(pixel[0], pixel[1], pixel[2]);
            results.add(result);
        }
        
        return aggregateResults(results);
    }
    
    /**
     * Aggregate multiple skin detections into one summary
     */
    private SkinDetectionResult aggregateResults(List<SkinDetectionResult> individualResults) {
        if (individualResults.isEmpty()) {
            return createNegativeResult(0f);
        }
        
        int fairSkinCount = 0;
        int fair1Count = 0;
        int fair2Count = 0;
        int fair3Count = 0;
        float totalConfidence = 0f;
        
        for (SkinDetectionResult result : individualResults) {
            if (result.isFairSkinDetected) {
                fairSkinCount++;
                totalConfidence += result.confidence;
                
                switch (result.skinType) {
                    case "FAIR_1": fair1Count++; break;
                    case "FAIR_2": fair2Count++; break;
                    case "FAIR_3": fair3Count++; break;
                }
            }
        }
        
        float percentage = ((float) fairSkinCount / individualResults.size()) * 100f;
        float avgConfidence = totalConfidence / fairSkinCount;
        
        // Determine dominant skin type
        String dominantType = "FAIR_3";
        if (fair1Count > fair2Count && fair1Count > fair3Count) {
            dominantType = "FAIR_1";
        } else if (fair2Count > fair1Count && fair2Count > fair3Count) {
            dominantType = "FAIR_2";
        }
        
        boolean detected = percentage > 10f; // Threshold minimal
        
        return new SkinDetectionResult(
            detected,
            dominantType,
            Math.min(avgConfidence, 0.98f),
            getHygieneNoteForPercentage(percentage),
            0f, 0f, 0f,
            0f
        );
    }
    
    /**
     * Analyze waste description menggunakan keyword-based approach
     * Fallback jika model tidak tersedia
     */
    public WasteAnalysisResult analyzeWaste(String text) {
        if (text == null || text.trim().isEmpty()) {
            return createDefaultWasteAnalysis();
        }
        
        String cleanText = text.toLowerCase().trim();
        
        // Keyword patterns untuk setiap kategori
        Map<String, Float> scores = new HashMap<>();
        
        // CONTAMINATED (CRITICAL)
        float contaminationScore = calculateKeywordScore(cleanText, new String[]{
            "terkontaminasi", "hair", "lantai", "cleaning chemical", 
            "tersentuh hands bare", "jatuh ke lantai", "paper towel kotor"
        });
        scores.put("CONTAMINATED", contaminationScore);
        
        // SPOILED (HIGH)
        float spoiledScore = calculateKeywordScore(cleanText, new String[]{
            "berjamur", "berbau busuk", "berlendir", "kebiruan", 
            "kehijauan", "busuk"
        });
        scores.put("SPOILED", spoiledScore);
        
        // EXPIRED (HIGH)
        float expiredScore = calculateKeywordScore(cleanText, new String[]{
            "expired", "kedaluwarsa", "lewat date", "lewat MHD", 
            "telah expired", "melewati expiry"
        });
        scores.put("EXPIRED", expiredScore);
        
        // OVERCOOKED (MEDIUM)
        float overcookedScore = calculateKeywordScore(cleanText, new String[]{
            "gosong", "kering", "overdone", "hangus", "hitam",
            "terlalu hard", "keras"
        });
        scores.put("OVERCOOKED", overcookedScore);
        
        // PREP_WASTE (LOW)
        float prepWasteScore = calculateKeywordScore(cleanText, new String[]{
            "trimming", "peeling", "potongan", "sisa", "prep",
            "cutting board", "sinking area"
        });
        scores.put("PREP_WASTE", prepWasteScore);
        
        // SURPLUS (LOW)
        float surplusScore = calculateKeywordScore(cleanText, new String[]{
            "tidak terjual", "surplus", "lebih", "excess", 
            "leftover", "tidak tersentuh", "unserved"
        });
        scores.put("SURPLUS", surplusScore);
        
        // Find best match
        String bestCategory = findBestCategory(scores);
        float bestScore = scores.get(bestCategory);
        
        String hygieneLevel = getHygieneLevel(bestCategory, bestScore);
        String action = getRecommendedAction(bestCategory);
        
        return new WasteAnalysisResult(
            bestCategory,
            hygieneLevel,
            bestScore,
            action,
            createCategoryBreakdown(scores)
        );
    }
    
    /**
     * Calculate keyword scoring
     */
    private float calculateKeywordScore(String text, String[] keywords) {
        float score = 0f;
        int matches = 0;
        
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score += 0.3f;
                matches++;
            }
        }
        
        // Boost score based on number of matching keywords
        if (matches >= 2) {
            score += 0.2f;
        }
        
        return Math.min(score, 1.0f);
    }
    
    /**
     * Find best category based on highest score
     */
    private String findBestCategory(Map<String, Float> scores) {
        String bestCategory = "UNKNOWN";
        float maxScore = 0f;
        
        for (Map.Entry<String, Float> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }
    
    /**
     * Get hygiene level string
     */
    private String getHygieneLevel(String category, float score) {
        switch (category) {
            case "CONTAMINATED":
                return score > 0.5f ? "🚨 CRITICAL ALERT!" : "⚠️ Needs Immediate Review";
            case "SPOILED":
            case "EXPIRED":
                return score > 0.6f ? "⚠️ HIGH PRIORITY" : "📋 Log as Waste";
            case "OVERCOOKED":
                return "📝 MEDIUM PRIORITY";
            default:
                return "ℹ️ LOW RISK";
        }
    }
    
    /**
     * Get recommended action
     */
    private String getRecommendedAction(String category) {
        switch (category) {
            case "CONTAMINATED":
                return "IMMEDIATE DISPOSAL REQUIRED - Create waste log now";
            case "SPOILED":
                return "Dispose according to organic waste SOP";
            case "EXPIRED":
                return "Remove from inventory and dispose";
            case "OVERCOOKED":
                return "Discard and adjust cooking procedure";
            case "PREP_WASTE":
                return "Log in prep waste tracking";
            case "SURPLUS":
                return "Document as surplus food (may donate if safe)";
            default:
                return "No action required";
        }
    }
    
    /**
     * Helper methods
     */
    private SkinDetectionResult createNegativeResult(float confidence) {
        return new SkinDetectionResult(
            false,
            "NONE",
            confidence,
            "No human skin detected - verify area",
            0f, 0f, 0f,
            0f
        );
    }
    
    private String getHygieneNoteForSkinType(String skinType) {
        switch (skinType) {
            case "FAIR_1":
                return "Very light skin detected - ensure proper glove usage";
            case "FAIR_2":
                return "Light skin detected - maintain hygiene standards";
            case "FAIR_3":
                return "Light skin with tan - normal observation";
            default:
                return "Skin type unknown";
        }
    }
    
    private String getHygieneNoteForPercentage(float percentage) {
        if (percentage > 50f) {
            return "High skin exposure - mandatory gloves and safety protocols";
        } else if (percentage > 20f) {
            return "Moderate skin exposure - observe hygiene practices";
        } else {
            return "Low skin exposure - normal activity observed";
        }
    }
    
    private WasteAnalysisResult createDefaultWasteAnalysis() {
        return new WasteAnalysisResult(
            "UNKNOWN",
            "ℹ️ NO DATA",
            0f,
            "Provide waste description for analysis",
            new HashMap<>()
        );
    }
    
    private HashMap<String, Float> createCategoryBreakdown(Map<String, Float> scores) {
        return new HashMap<>(scores);
    }
    
    // Result Data Classes
    public static class SkinDetectionResult {
        public final boolean isFairSkinDetected;
        public final String skinType;
        public final float confidence;
        public final String hygieneNote;
        public final float r, g, b, avgIntensity;
        
        public SkinDetectionResult(boolean detected, String type, float conf, 
                                   String note, float r, float g, float b, float avg) {
            this.isFairSkinDetected = detected;
            this.skinType = type;
            this.confidence = conf;
            this.hygieneNote = note;
            this.r = r;
            this.g = g;
            this.b = b;
            this.avgIntensity = avg;
        }
        
        @Override
        public String toString() {
            return String.format("Skin Detected: %b | Type: %s | Confidence: %.1f%%",
                               isFairSkinDetected, skinType, confidence * 100);
        }
    }
    
    public static class WasteAnalysisResult {
        public final String category;
        public final String hygieneLevel;
        public final float confidence;
        public final String recommendedAction;
        public final Map<String, Float> categoryScores;
        
        public WasteAnalysisResult(String cat, String level, float conf, 
                                   String action, Map<String, Float> scores) {
            this.category = cat;
            this.hygieneLevel = level;
            this.confidence = conf;
            this.recommendedAction = action;
            this.categoryScores = scores;
        }
        
        @Override
        public String toString() {
            return String.format("Category: %s | Hygiene: %s | Score: %.1f%%",
                               category, hygieneLevel, confidence * 100);
        }
    }
}
