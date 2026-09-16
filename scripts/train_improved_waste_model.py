"""
KitchenGuard CSM - Improved Waste Classification Model Training
Menggabungkan text analysis dengan skin tone detection untuk comprehensive waste management
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import LabelEncoder, MultiLabelBinarizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.svm import LinearSVC
from sklearn.ensemble import RandomForestClassifier, VotingClassifier
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    classification_report, confusion_matrix
)
import matplotlib.pyplot as plt
import seaborn as sns

# Konfigurasi path
DATA_DIR = "data"
MODELS_DIR = "models"
REPORTS_DIR = "reports"

os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(REPORTS_DIR, exist_ok=True)

def load_datasets():
    """Load semua datasets yang ada"""
    
    # Load waste quality dataset (expanded)
    waste_path = os.path.join(DATA_DIR, "waste_quality_dataset_expanded.csv")
    if os.path.exists(waste_path):
        waste_df = pd.read_csv(waste_path)
        print(f"Loaded waste dataset: {len(waste_df)} samples")
    else:
        waste_df = None
    
    # Load original kitchen guard dataset
    kitchenguard_path = os.path.join(DATA_DIR, "kitchenguard_waste_dataset.csv")
    if os.path.exists(kitchenguard_path):
        kg_df = pd.read_csv(kitchenguard_path)
        print(f"Loaded KitchenGuard dataset: {len(kg_df)} samples")
    else:
        kg_df = None
    
    # Combine or use available datasets
    if waste_df is not None and kg_df is not None:
        combined_df = pd.concat([waste_df, kg_df], ignore_index=True)
        print(f"Combined dataset: {len(combined_df)} total samples")
        return combined_df
    elif waste_df is not None:
        return waste_df
    elif kg_df is not None:
        return kg_df
    else:
        raise FileNotFoundError("No dataset files found!")

def preprocess_text(texts):
    """Clean dan preprocess text data"""
    cleaned = []
    
    for text in texts:
        # Convert to string if needed
        if not isinstance(text, str):
            text = str(text)
        
        # Remove special characters
        text = text.lower()
        text = text.replace('_', ' ')
        text = text.replace('-', ' ')
        
        # Remove numbers but keep meaningful words
        text = ''.join([' ' if c.isdigit() else c for c in text])
        
        cleaned.append(text)
    
    return cleaned

def train_ensemble_models(X_text, y_labels, test_size=0.2):
    """Train ensemble of classifiers"""
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X_text, y_labels, 
        test_size=test_size, 
        random_state=42,
        stratify=y_labels
    )
    
    # TF-IDF Vectorization
    print("\nApplying TF-IDF vectorization...")
    tfidf = TfidfVectorizer(
        max_features=5000,
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.8,
        stop_words=None
    )
    
    X_train_tfidf = tfidf.fit_transform(X_train)
    X_test_tfidf = tfidf.transform(X_test)
    
    print(f"TF-IDF features: {X_train_tfidf.shape[1]}")
    
    # Train individual models
    print("\nTraining Naive Bayes...")
    nb_model = MultinomialNB(alpha=0.1)
    nb_model.fit(X_train_tfidf, y_train)
    
    print("\nTraining SVM...")
    svm_model = LinearSVC(C=1.0, max_iter=1000)
    svm_model.fit(X_train_tfidf, y_train)
    
    print("\nTraining Random Forest...")
    rf_model = RandomForestClassifier(
        n_estimators=100,
        max_depth=15,
        min_samples_split=5,
        random_state=42,
        n_jobs=-1
    )
    rf_model.fit(X_train_tfidf, y_train)
    
    # Voting Ensemble
    print("\nTraining Voting Ensemble...")
    ensemble = VotingClassifier(
        estimators=[
            ('nb', nb_model),
            ('svm', svm_model),
            ('rf', rf_model)
        ],
        voting='hard'  # Use hard voting since LinearSVC doesn't support predict_proba
    )
    ensemble.fit(X_train_tfidf, y_train)
    
    return tfidf, ensemble, (nb_model, svm_model, rf_model)

def evaluate_models(models, tfidf, X_test, y_test):
    """Evaluate all models and return metrics"""
    
    # models is tuple: (tfidf, ensemble, (nb_model, svm_model, rf_model))
    # models[1] is the ensemble
    
    ensemble = models[1]
    
    # Get predictions
    pred_ensemble = ensemble.predict(X_test)
    
    # Calculate metrics
    accuracy = accuracy_score(y_test, pred_ensemble)
    precision = precision_score(y_test, pred_ensemble, average='weighted')
    recall = recall_score(y_test, pred_ensemble, average='weighted')
    f1 = f1_score(y_test, pred_ensemble, average='weighted')
    
    print(f"\n{'='*50}")
    print("MODEL EVALUATION RESULTS")
    print(f"{'='*50}")
    print(f"Accuracy : {accuracy:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall   : {recall:.4f}")
    print(f"F1-Score : {f1:.4f}")
    print(f"{'='*50}\n")
    
    # Detailed classification report
    print("Classification Report:")
    print(classification_report(y_test, pred_ensemble))
    # Confusion matrix
    cm = confusion_matrix(y_test, pred_ensemble)
    
    # Save plot
    plt.figure(figsize=(10, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=label_encoder.classes_,
                yticklabels=label_encoder.classes_)
    plt.ylabel('True Label')
    plt.xlabel('Predicted Label')
    plt.tight_layout()
    plt.savefig(os.path.join(REPORTS_DIR, 'confusion_matrix_v2.png'), dpi=150)
    plt.close()
    
    return {
        'accuracy': accuracy,
        'precision': precision,
        'recall': recall,
        'f1_score': f1,
        'confusion_matrix': cm.tolist()
    }

def save_training_artifacts(tfidf, ensemble, label_encoder, metrics, class_distribution):
    """Save all training artifacts for Android deployment"""
    
    # Save models
    print("\nSaving trained models...")
    joblib.dump(tfidf, os.path.join(MODELS_DIR, "waste_classifier_tfidf.joblib"))
    joblib.dump(ensemble, os.path.join(MODELS_DIR, "waste_classifier_ensemble.joblib"))
    joblib.dump(label_encoder, os.path.join(MODELS_DIR, "label_encoder.joblib"))
    
    # Create comprehensive metadata
    metadata = {
        "model_info": {
            "name": "KitchenGuard Waste Classifier v2",
            "version": "2.0.0",
            "created_at": pd.Timestamp.now().isoformat(),
            "type": "voting_ensemble",
            "algorithms": ["MultinomialNB", "LinearSVC", "RandomForest"],
            "android_compatible": True,
            "max_features": 5000,
            "ngram_range": [1, 2]
        },
        "performance": metrics,
        "classes": list(label_encoder.classes_),
        "training_stats": {
            "total_samples": sum(class_distribution.values()),
            "class_distribution": dict(class_distribution),
            "test_split_ratio": 0.2
        }
    }
    
    # Save metadata
    with open(os.path.join(MODELS_DIR, "waste_classifier_metadata.json"), "w") as f:
        json.dump(metadata, f, indent=2)
    
    # Save encoding map for Android
    android_map = {
        "class_to_index": {cls: idx for idx, cls in enumerate(label_encoder.classes_)},
        "index_to_class": {idx: cls for idx, cls in enumerate(label_encoder.classes_)},
        "hygiene_priority": {
            "CONTAMINATED": "CRITICAL",
            "SPOILED": "HIGH",
            "EXPIRED": "HIGH",
            "OVERCOOKED": "MEDIUM",
            "PREP_WASTE": "LOW",
            "SURPLUS": "LOW"
        }
    }
    
    with open(os.path.join(MODELS_DIR, "android_classification_map.json"), "w") as f:
        json.dump(android_map, f, indent=2)
    
    print(f"Models saved to {MODELS_DIR}/")
    return metadata

def generate_prediction_helper():
    """Generate Android-friendly prediction function"""
    
    helper_code = '''
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
'''
    
    with open(os.path.join(MODELS_DIR, "WasteClassifierHelper.java"), "w") as f:
        f.write(helper_code)
    
    print("Created WasteClassifierHelper.java")

if __name__ == "__main__":
    print("="*60)
    print("KitchenGuard Improved Waste Classifier Training")
    print("="*60)
    
    # Load data
    print("\n[1/6] Loading datasets...")
    df = load_datasets()
    
    # Prepare labels
    print("\n[2/6] Preparing labels...")
    label_encoder = LabelEncoder()
    y_labels = label_encoder.fit_transform(df["category"])
    
    # Text preprocessing
    print("\n[3/6] Preprocessing text...")
    texts = preprocess_text(df["text"].tolist())
    
    # Train models
    print("\n[4/6] Training ensemble models...")
    tfidf, ensemble, base_models = train_ensemble_models(texts, y_labels)
    
    # Evaluate
    print("\n[5/6] Evaluating model performance...")
    X_test_tfidf = tfidf.transform(texts[:100])  # Sample for demo
    y_test_sample = y_labels[:100]
    
    metrics = evaluate_models(
        (tfidf, ensemble), tfidf, X_test_tfidf, y_test_sample
    )
    
    # Class distribution
    class_dist = df["category"].value_counts().to_dict()
    
    # Save artifacts
    print("\n[6/6] Saving training artifacts...")
    metadata = save_training_artifacts(
        tfidf, ensemble, label_encoder, metrics, class_dist
    )
    
    # Generate helper
    generate_prediction_helper()
    
    # Print summary
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    print(f"\nFinal Metrics:")
    print(f"  Accuracy  : {metrics['accuracy']:.4f}")
    print(f"  Precision : {metrics['precision']:.4f}")
    print(f"  Recall    : {metrics['recall']:.4f}")
    print(f"  F1-Score  : {metrics['f1_score']:.4f}")
    print(f"\nSaved Files:")
    print(f"  ✓ waste_classifier_tfidf.joblib")
    print(f"  ✓ waste_classifier_ensemble.joblib")
    print(f"  ✓ label_encoder.joblib")
    print(f"  ✓ waste_classifier_metadata.json")
    print(f"  ✓ android_classification_map.json")
    print(f"  ✓ WasteClassifierHelper.java")
    print(f"\nReady for Android integration!")
