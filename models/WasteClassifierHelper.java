package com.kitchenguard.csm.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Waste Classifier Helper untuk Android Client
 * Selaras dengan android_class_map.json dan backend FastAPI KitchenGuard CSM.
 *
 * Pemetaan Indeks Kelas (Resmi):
 * 0: CONTAMINATED
 * 1: EXPIRED
 * 2: OVERCOOKED
 * 3: PREP_WASTE
 * 4: SPOILED
 * 5: SURPLUS
 */
public class WasteClassifierHelper {
    
    private Map<String, Integer> classToIndex;
    private int[] indexToClass;
    private float contaminationThreshold = 0.6f;
    private float spoiledThreshold = 0.7f;
    
    public WasteClassifierHelper() {
        loadClassMapping();
    }
    
    public String[] getCategories() {
        return new String[]{
            "CONTAMINATED", "EXPIRED", "OVERCOOKED", "PREP_WASTE", "SPOILED", "SURPLUS"
        };
    }
    
    public PredictionResult analyzeWaste(String text) {
        String cleanText = preprocessText(text);
        float[] predictions = runCalibratedInference(cleanText);
        
        int predictedClass = findMaxIndex(predictions);
        float confidence = predictions[predictedClass];
        
        String categoryName = getCategoryName(predictedClass);
        String hygieneLevel = getHygieneLevel(categoryName, confidence);
        
        return new PredictionResult(
            categoryName, hygieneLevel, confidence, createPredictionDetails(predictions)
        );
    }
    
    private float[] runCalibratedInference(String text) {
        float[] scores = new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        if (text == null || text.trim().isEmpty()) return normalize(scores);

        if (text.contains("kontaminasi") || text.contains("terkontaminasi") || text.contains("rambut") ||
            text.contains("hair") || text.contains("lantai") || text.contains("kotor") ||
            text.contains("chemical") || text.contains("kimia") || text.contains("beling") ||
            text.contains("kaca") || text.contains("lalat")) {
            scores[0] += 5.0f;
        }
        if (text.contains("expired") || text.contains("kadaluarsa") || text.contains("lewat") ||
            text.contains("mhd") || text.contains("tanggal") || text.contains("basi")) {
            scores[1] += 5.0f;
        }
        if (text.contains("gosong") || text.contains("hangus") || text.contains("keras") ||
            text.contains("terbakar") || text.contains("overcooked") || text.contains("overdone")) {
            scores[2] += 5.0f;
        }
        if (text.contains("kupasan") || text.contains("kulit") || text.contains("bonggol") ||
            text.contains("potongan") || text.contains("prep") || text.contains("trimming") ||
            text.contains("batang") || text.contains("akar")) {
            scores[3] += 5.0f;
        }
        if (text.contains("busuk") || text.contains("lendir") || text.contains("berlendir") ||
            text.contains("bau") || text.contains("tengik") || text.contains("jamur") ||
            text.contains("berjamur") || text.contains("asam") || text.contains("lembek")) {
            scores[4] += 5.0f;
        }
        if (text.contains("surplus") || text.contains("tidak habis") || text.contains("unserved") ||
            text.contains("leftover") || text.contains("berlebih") || text.contains("porsi lebih") ||
            text.contains("sisa saji")) {
            scores[5] += 5.0f;
        }

        return normalize(scores);
    }
    
    private float[] normalize(float[] scores) {
        float sum = 0f;
        for (float s : scores) sum += Math.exp(s);
        float[] probs = new float[scores.length];
        for (int i = 0; i < scores.length; i++) probs[i] = (float) (Math.exp(scores[i]) / sum);
        return probs;
    }
    
    private String getHygieneLevel(String category, float confidence) {
        switch (category) {
            case "CONTAMINATED": return confidence > contaminationThreshold ? "CRITICAL ALERT!" : "NEEDS REVIEW";
            case "SPOILED":
            case "EXPIRED": return confidence > spoiledThreshold ? "HIGH PRIORITY" : "MODERATE RISK";
            case "OVERCOOKED": return "MEDIUM PRIORITY";
            default: return "LOW RISK";
        }
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
        this.classToIndex.put("CONTAMINATED", 0);
        this.classToIndex.put("EXPIRED", 1);
        this.classToIndex.put("OVERCOOKED", 2);
        this.classToIndex.put("PREP_WASTE", 3);
        this.classToIndex.put("SPOILED", 4);
        this.classToIndex.put("SURPLUS", 5);
        this.indexToClass = new int[]{0, 1, 2, 3, 4, 5};
    }
    
    public static class PredictionResult {
        public final String category;
        public final String hygieneLevel;
        public final float confidence;
        public final JSONObject details;
        
        public PredictionResult(String category, String hygieneLevel, float confidence, JSONObject details) {
            this.category = category;
            this.hygieneLevel = hygieneLevel;
            this.confidence = confidence;
            this.details = details;
        }
    }
}
