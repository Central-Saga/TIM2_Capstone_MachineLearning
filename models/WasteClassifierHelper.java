
package com.kitchenguard.csm.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Waste Classifier Helper untuk Android
 * Mendeteksi kategori limbah dari deskripsi teks
 */
public class WasteClassifierHelper {
    
    private Map<String, Integer> classToIndex;
    private int[] indexToClass;
    private float contaminationThreshold = 0.6f;
    private float spoiledThreshold = 0.7f;
    
    public WasteClassifierHelper() {
        loadClassMapping();
    }
    
    /**
     * Analyze waste description and categorize
     * @param text Description from kitchen staff
     * @return PredictionResult with category and confidence
     */
    public PredictionResult analyzeWaste(String text) {
        // Preprocess text
        String cleanText = preprocessText(text);
        
        // Apply TF-IDF feature extraction
        float[] tfidfFeatures = extractTFIDFFeatures(cleanText);
        
        // Run model inference (placeholder - replace with actual model loading)
        float[] predictions = runModelInference(tfidfFeatures);
        
        // Find best prediction
        int predictedClass = findMaxIndex(predictions);
        float confidence = predictions[predictedClass];
        
        String categoryName = getClassName(predictedClass);
        String hygieneLevel = getHygieneLevel(categoryName, confidence);
        
        return new PredictionResult(
            categoryName, 
            hygieneLevel, 
            confidence,
            createPredictionDetails(predictions)
        );
    }
    
    /**
     * Quick analysis based on keywords (fallback)
     */
    public QuickAnalysis detectQuick(String text) {
        text = text.toLowerCase();
        
        // Check for critical indicators
        boolean hasContaminationKeywords = 
            text.contains("terkontaminasi") || 
            text.contains("hair") || 
            text.contains("lantai") ||
            text.contains("cleaning chemical");
        
        boolean hasSpoiledKeywords = 
            text.contains("berjamur") || 
            text.contains("berbau busuk") ||
            text.contains("berlendir") ||
            text.contains("expired");
        
        boolean hasExpiredKeywords = 
            text.contains("expired") || 
            text.contains("lewat date") ||
            text.contains("kedaluwarsa");
        
        boolean hasOvercookedKeywords = 
            text.contains("gosong") || 
            text.contains("kering") ||
            text.contains("overdone") ||
            text.contains("hangus");
        
        return new QuickAnalysis(
            hasContaminationKeywords ? "CRITICAL" : null,
            hasSpoiledKeywords || hasExpiredKeywords ? "HIGH" : null,
            hasOvercookedKeywords ? "MEDIUM" : null
        );
    }
    
    private String getHygieneLevel(String category, float confidence) {
        switch (category) {
            case "CONTAMINATED":
                return confidence > contaminationThreshold ? "CRITICAL ALERT!" : "NEEDS REVIEW";
            case "SPOILED":
            case "EXPIRED":
                return confidence > spoiledThreshold ? "HIGH PRIORITY" : "MODERATE RISK";
            case "OVERCOOKED":
                return "MEDIUM PRIORITY";
            default:
                return "LOW RISK";
        }
    }
    
    private String[] getCategories() {
        return new String[]{"SPOILED", "EXPIRED", "PREP_WASTE", 
                          "OVERCOOKED", "CONTAMINATED", "SURPLUS"};
    }
    
    private int findMaxIndex(float[] probabilities) {
        int maxIndex = 0;
        float maxProb = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    private String getCategoryName(int index) {
        String[] categories = getCategories();
        return (index >= 0 && index < categories.length) ? categories[index] : "UNKNOWN";
    }
    
    private String preprocessText(String text) {
        if (text == null || text.isEmpty()) return "";
        text = text.toLowerCase().trim();
        text = text.replace("_", " ");
        text = text.replace("-", " ");
        return text;
    }
    
    // Placeholder methods - implement actual TF-IDF logic
    private float[] extractTFIDFFeatures(String text) {
        // Implement TF-IDF feature extraction
        return new float[5000]; // Placeholder
    }
    
    private float[] runModelInference(float[] features) {
        // Call ML model for prediction
        return new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f}; // Placeholder
    }
    
    private JSONObject createPredictionDetails(float[] probabilities) {
        JSONObject details = new JSONObject();
        String[] categories = getCategories();
        for (int i = 0; i < categories.length; i++) {
            try {
                details.put(categories[i], probabilities[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return details;
    }
    
    private void loadClassMapping() {
        this.classToIndex = new HashMap<>();
        this.classToIndex.put("SPOILED", 0);
        this.classToIndex.put("EXPIRED", 1);
        this.classToIndex.put("PREP_WASTE", 2);
        this.classToIndex.put("OVERCOOKED", 3);
        this.classToIndex.put("CONTAMINATED", 4);
        this.classToIndex.put("SURPLUS", 5);
        
        this.indexToClass = new int[]{0, 1, 2, 3, 4, 5};
    }
    
    // Result classes
    public static class PredictionResult {
        public final String category;
        public final String hygieneLevel;
        public final float confidence;
        public final JSONObject details;
        
        public PredictionResult(String category, String hygieneLevel, 
                               float confidence, JSONObject details) {
            this.category = category;
            this.hygieneLevel = hygieneLevel;
            this.confidence = confidence;
            this.details = details;
        }
        
        @Override
        public String toString() {
            return String.format("Category: %s | Hygiene: %s | Confidence: %.2f%%",
                               category, hygieneLevel, confidence * 100);
        }
    }
    
    public static class QuickAnalysis {
        public String criticalWarning;
        public String highPriority;
        public String mediumPriority;
        
        public QuickAnalysis(String critical, String high, String medium) {
            this.criticalWarning = critical;
            this.highPriority = high;
            this.mediumPriority = medium;
        }
    }
}
