
package com.kitchenguard.csm.utils;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Skin Tone Detection Helper untuk Android
 * Mendeteksi kategori (HAND/FACE) dan skin type (FAIR_1/FAIR_2/FAIR_3)
 */
public class SkinDetectorHelper {
    
    private Interpreter tflite;
    private int[] categoryClasses;
    private int[] skinTypeClasses;
    
    public SkinDetectorHelper(Context context) throws IOException {
        // Load TFLite model (akan di-generate kemudian)
        ByteBuffer buffer = FileUtil.loadMappedFile(context, "skin_model.tflite");
        tflite = new Interpreter(buffer);
        
        // Load label mappings (aligned with android_encoding_map.json: 0=FACE, 1=HAND)
        this.categoryClasses = new int[]{0, 1}; // 0: FACE, 1: HAND
        this.skinTypeClasses = new int[]{0, 1, 2}; // FAIR_1, FAIR_2, FAIR_3
    }
    
    /**
     * Detect skin from image features (RGB values)
     * @param rgbMean R, G, B mean values from image
     * @param rgbStd R, G, B standard deviation
     * @return PredictionResult with category and skin type
     */
    public PredictionResult detect(float[] rgbMean, float[] rgbStd) {
        // Prepare input tensor (6 features)
        float[][] inputArray = new float[1][6];
        inputArray[0][0] = rgbMean[0];
        inputArray[0][1] = rgbMean[1];
        inputArray[0][2] = rgbMean[2];
        inputArray[0][3] = rgbStd[0];
        inputArray[0][4] = rgbStd[1];
        inputArray[0][5] = rgbStd[2];
        
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(24);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (int i = 0; i < 6; i++) {
            inputBuffer.putFloat(inputArray[0][i]);
        }
        inputBuffer.rewind();
        
        // Run inference
        float[][] outputCategory = new float[1][2];
        float[][] outputSkin = new float[1][4];
        
        tflite.run(inputBuffer, outputCategory);
        tflite.run(inputBuffer, outputSkin);
        
        // Find predictions
        int predictedCategory = getMaxIndex(outputCategory[0]);
        int predictedSkin = getMaxIndex(outputSkin[0]);
        
        String categoryName = getCategoryName(predictedCategory);
        String skinTypeName = getSkinTypeName(predictedSkin);
        
        return new PredictionResult(categoryName, skinTypeName, 
                                   outputCategory[0][predictedCategory],
                                   outputSkin[0][predictedSkin]);
    }
    
    private int getMaxIndex(float[] probabilities) {
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
        switch(index) {
            case 0: return "HAND";
            case 1: return "FACE";
            default: return "UNKNOWN";
        }
    }
    
    private String getSkinTypeName(int index) {
        switch(index) {
            case 0: return "FAIR_1";
            case 1: return "FAIR_2";
            case 2: return "FAIR_3";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Simple prediction without TFLite (fallback menggunakan logic sederhana)
     * Berdasarkan RGB threshold
     */
    public FallbackPredictionResult detectSimple(float r, float g, float b) {
        // Fair skin biasanya memiliki RGB values tinggi (> 200)
        boolean isFairSkin = r > 200 && g > 180 && b > 160;
        
        String skinType = isFairSkin ? "FAIR" : "OTHER";
        float confidence = isFairSkin ? 0.85f : 0.15f;
        
        return new FallbackPredictionResult(skinType, confidence);
    }
    
    public static class PredictionResult {
        public String category;
        public String skinType;
        public float categoryConfidence;
        public float skinConfidence;
        
        public PredictionResult(String category, String skinType, 
                               float catConf, float skinConf) {
            this.category = category;
            this.skinType = skinType;
            this.categoryConfidence = catConf;
            this.skinConfidence = skinConf;
        }
        
        @Override
        public String toString() {
            return "Category: " + category + ", Skin: " + skinType + 
                   " (confidence: " + skinConfidence + ")";
        }
    }
    
    public static class FallbackPredictionResult {
        public String skinType;
        public float confidence;
        
        public FallbackPredictionResult(String skinType, float confidence) {
            this.skinType = skinType;
            this.confidence = confidence;
        }
    }
}
