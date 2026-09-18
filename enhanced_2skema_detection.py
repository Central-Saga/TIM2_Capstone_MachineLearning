#!/usr/bin/env python3
"""
KitchenGuard ML - Enhanced with Unknown/Unknown Material Detection
Implementasi 2-skema scanning:
1. CONFIRMED = Bahan terlihat dan dikenal -> Classify
2. UNKNOWN = Bahan tidak jelas/tidak dikenal -> Reject
"""

import os
import sys
import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics.pairwise import cosine_similarity

# Load model artifacts
print("="*80)
print("KITCHENGUARD ML - 2-SKEMA ENHANCEMENT")
print("="*80)

try:
    # Load models
    vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
    classifier = joblib.load('models/waste_classification/waste_classifier_model.joblib')
    encoder = joblib.load('models/waste_classification/label_encoder.joblib')
    
    # Load metadata for training references
    with open('models/waste_classification/metadata.json', 'r') as f:
        import json
        metadata = json.load(f)
    
    print("\nModels loaded successfully!")
    print("Categories:", metadata['classes'])
    
except Exception as e:
    print("\nError loading models: {0}".format(e))
    exit(1)

# Define known material patterns per category
known_patterns = {
    "SPOILED": [
        "berbau busuk", "berlendir", "berjamur", "busuk", "membusuk", 
        "berubah kehitaman", "berbau asam", "telah berjamur", "menggelembung"
    ],
    "EXPIRED": [
        "expired", "kedaluwarsa", "lewat date", "MHD terlewati", 
        "telah melewati", "expired date", "batas tanggal konsumsi"
    ],
    "PREP_WASTE": [
        "kulit", "bonggol", "sisa kupasan", "trimming waste", 
        "peeling", "preparation waste", "sisa pemecahan telur", "biji melon",
        "bawang merah", "tomato stem"
    ],
    "OVERCOOKED": [
        "gosong", "hangus", "terbakar", "overdone", "burnt", 
        "terlalu lama", "kelewat matang", "kekurangan air", "kerak"
    ],
    "CONTAMINATED": [
        "terkontaminasi", "jatuh ke lantai", "tercemarkan", 
        "kemasukan", "tersentuh benda asing", "hair", "insect",
        "debu kotor", "sabun cuci", "air kotor bocoran"
    ],
    "SURPLUS": [
        "kelebihan", "sisa buffet", "tidak terjual", "lebih awal",
        "overshoot", "leftover", "tidak habis dikonsumsi", "excess portion"
    ]
}

def count_pattern_matches(text, patterns):
    """Count how many patterns match in text"""
    text_lower = text.lower()
    matches = sum(1 for p in patterns if p in text_lower)
    return matches

def detect_material_status(text):
    """
    Detect whether material is KNOWN or UNKNOWN
    
    Returns dict with status, category, confidence, reason
    """
    text_lower = text.lower()
    
    # Step 1: Check keyword pattern matching
    pattern_scores = {}
    for category, patterns in known_patterns.items():
        matches = count_pattern_matches(text_lower, patterns)
        pattern_scores[category] = matches
    
    max_category = max(pattern_scores, key=pattern_scores.get)
    max_pattern_score = pattern_scores[max_category]
    
    # Step 2: Simple preprocessing
    def simple_preprocess(t):
        return t.lower().replace('_', ' ').replace('-', ' ')
    
    clean_text = simple_preprocess(text)
    
    # Step 3: Get ML prediction
    try:
        features = vectorizer.transform([clean_text])
        ml_prediction = classifier.predict(features)[0]
        ml_confidence = classifier.predict_proba(features)[0][ml_prediction] * 100
        
        # Convert label to category name
        ml_category = encoder.inverse_transform([ml_prediction])[0]
        
    except Exception as e:
        print("ML Prediction Error: {0}".format(e))
        ml_confidence = 0
        ml_category = None
    
    # Step 4: Decision Logic for 2-skema
    PATTERN_THRESHOLD = 1  # At least 1 matching keyword
    ML_CONFIDENCE_THRESHOLD = 75  # Minimum ML confidence percentage
    
    total_keyword_matches = sum(pattern_scores.values())
    
    # LOGIC: BOTH pattern matching AND ML confidence must agree
    if max_pattern_score >= PATTERN_THRESHOLD and ml_confidence >= ML_CONFIDENCE_THRESHOLD:
        # BOTH confirm -> CONFIRMED
        status = "CONFIRMED"
        final_category = ml_category
        confidence = max(ml_confidence, max_pattern_score * 25)
        
        reason = ("Keyword match: {0}/{1} patterns in '{2}' | ML confidence: {3:.1f}%".format(
            max_pattern_score, len(known_patterns[max_category]), max_category, ml_confidence))
    
    elif max_pattern_score < PATTERN_THRESHOLD or ml_confidence < ML_CONFIDENCE_THRESHOLD:
        # One or both fail -> UNKNOWN
        status = "UNKNOWN"
        final_category = "UNCATEGORIZED"
        confidence = min(ml_confidence, max_pattern_score * 25) if ml_category else 0
        
        reasons = []
        if max_pattern_score < PATTERN_THRESHOLD:
            reasons.append("no clear keywords detected")
        if ml_confidence < ML_CONFIDENCE_THRESHOLD:
            reasons.append("ML confidence too low ({0:.1f}%)".format(ml_confidence))
        
        reason = "Bahan tidak dapat diidentifikasi karena: " + "; ".join(reasons)
    
    else:
        status = "UNKNOWN"
        final_category = "UNCATEGORIZED"
        confidence = 0
        reason = "Insufficient evidence for classification"
    
    return {
        'status': status,
        'category': final_category,
        'confidence': confidence,
        'reason': reason
    }

# Test both skemas
print("\n" + "="*80)
print("TESTING 2-SKEMA SYSTEM")
print("="*80)

test_cases = [
    # Skema 1: CONFIRMED (should be classified)
    ("Daging sapi tenderloin berbau busuk asam dan berlendir", "KNOWN SPOILED"),
    ("Saus mayonnaise botol sudah lewat expired date seminggu lalu", "KNOWN EXPIRED"),
    ("Kulit wortel dan bonggol brokoli sisa persiapan prep station", "KNOWN PREP_WASTE"),
    ("Ayam goreng gosong hangus hitam karena kompor ditinggal", "KNOWN OVERCOOKED"),
    ("Ikan fillet jatuh ke lantai berminyak dekat sink", "KNOWN CONTAMINATED"),
    ("Nasi tumpeng sisa buffet gathering yang tidak habis dimakan", "KNOWN SURPLUS"),
    
    # Skema 2: UNKNOWN (should be rejected)
    ("Barang rusak", "UNKNOWN - Too vague"),
    ("Material tidak dikenali", "UNKNOWN - No keywords"),
    ("Xyz abc def ghi jkl mno", "UNKNOWN - Gibberish"),
    ("Benda aneh warna biru", "UNKNOWN - Not waste-related"),
    ("Sisa makanan", "UNKNOWN - Too general"),
]

results = {
    'confirmed': [],
    'unknown': []
}

print("\nTest Case                                          | Expected          | Result      | Conf")
print("-"*105)

for text, expected_desc in test_cases:
    result = detect_material_status(text)
    
    # Display
    display_text = text[:48] + ".." if len(text) > 50 else text
    result_str = "{0} -> {1}".format(result['status'], result['category'])
    conf_str = "{0:.1f}%".format(result['confidence'])
    
    print("{0:<50} | {1:<20} | {2:<12} | {3}".format(display_text, expected_desc, result_str, conf_str))
    
    # Store results
    if result['status'] == 'CONFIRMED':
        results['confirmed'].append({
            'text': text,
            'category': result['category'],
            'confidence': result['confidence']
        })
    else:
        results['unknown'].append({
            'text': text,
            'reason': result['reason']
        })

# Summary statistics
total_tests = len(test_cases)
confirmed_count = len(results['confirmed'])
unknown_count = len(results['unknown'])

print("\n" + "="*105)
print("TEST SUMMARY")
print("="*105)
print("\nTotal Tests: {0}".format(total_tests))
print("Confirmed Materials: {0} ({1:.1f}%)".format(confirmed_count, confirmed_count/total_tests*100))
print("Rejected (Unknown): {0} ({1:.1f}%)".format(unknown_count, unknown_count/total_tests*100))

# Detailed breakdown
if results['confirmed']:
    print("\nCONFIRMED Cases:")
    for case in results['confirmed']:
        print("  [{0}] {1}".format(case['category'], case['text'][:60]))
        print("      Confidence: {0:.1f}%".format(case['confidence']))

if results['unknown']:
    print("\nUNKNOWN/REJECTED Cases:")
    for case in results['unknown']:
        print("  {0}".format(case['text'][:60]))
        print("      Reason: {0}".format(case['reason']))

# Dataset retraining plan
print("\n" + "="*80)
print("DATASET RETRAINING STRATEGY")
print("="*80)

retrain_plan = """
GOAL: Retrain model with enhanced labeled patterns + rejection learning

CURRENT DATASET:
- Samples: 1,078 (balanced across 6 categories)
- Quality: Good, synthetic but representative
- Gap: Lacks negative samples (things that shouldn't be classified)

RETRAINING PLAN:
Step 1: Add Negative/Rejection Samples
  Category: "UNIDENTIFIABLE"
  Examples:
    - "barang rusak"
    - "material tidak dikenali"
    - "xyz abc def random"
    - "benda aneh"
    - "sesuatu yang rusak"
  Count: ~200 samples

Step 2: Expand Keyword Coverage
  - Add more varied patterns per existing category
  - Include synonyms and related terms
  - Ensure edge cases are covered

Step 3: Implement Confidence Threshold System
  - Pattern matching threshold: 1 keyword minimum
  - ML confidence threshold: 75% minimum
  - Both must pass for CONFIRMED status

Step 4: Validate on Hold-out Test Set
  - Reserve 15% of new data for testing
  - Ensure unknown detection works correctly
  - Measure precision/recall for each skema
"""

print(retrain_plan)

# Android implementation guide
print("\n" + "="*80)
print("ANDROID IMPLEMENTATION GUIDE")
print("="*80)

android_guide = """
// WasteClassifierHelper.java - Updated for 2-skema
public class WasteClassifierHelper {
    
    // Pattern database
    private Map<String, List<String>> knownPatterns = new HashMap<>();
    private static final int PATTERN_THRESHOLD = 1;
    private static final float ML_CONFIDENCE_THRESHOLD = 0.75f;
    
    public PredictionResult analyzeWaste(String text) {
        // Step 1: Check keyword patterns
        String[] categories = getCategories();
        int bestPatternScore = 0;
        String bestCategory = null;
        
        for (String cat : categories) {
            int score = countPatternMatches(text, getPatterns(cat));
            if (score > bestPatternScore) {
                bestPatternScore = score;
                bestCategory = cat;
            }
        }
        
        // Step 2: If no patterns match -> REJECT immediately
        if (bestPatternScore < PATTERN_THRESHOLD) {
            return new PredictionResult(
                "UNKNOWN",
                "Material cannot be identified",
                0.0f,
                false  // isConfirmed = false
            );
        }
        
        // Step 3: Run ML model
        float[] predictions = runModelInference(text);
        float maxConfidence = findMax(predictions);
        int predictedIndex = findMaxIndex(predictions);
        
        // Step 4: Double-check with ML confidence
        if (maxConfidence < ML_CONFIDENCE_THRESHOLD) {
            return new PredictionResult(
                "UNKNOWN",
                "Low confidence: " + (maxConfidence*100) + "%",
                maxConfidence,
                false  // isConfirmed = false
            );
        }
        
        // Step 5: Both confirm -> APPROVED
        return new PredictionResult(
            categories[predictedIndex],
            "Valid waste item",
            maxConfidence,
            true  // isConfirmed = true
        );
    }
    
    private int countPatternMatches(String text, List<String> patterns) {
        text = text.toLowerCase();
        int matches = 0;
        for (String pattern : patterns) {
            if (text.contains(pattern.toLowerCase())) {
                matches++;
            }
        }
        return matches;
    }
}
"""

print(android_guide)

# Final conclusion
print("\n" + "="*80)
print("FINAL ASSESSMENT")
print("="*80)

print("""
RESULTS:

Testing Results:
- Confirmed Materials: {0}/{1}
- Rejected Unknowns: {2}/{1}

How It Works:
1. Input text checked against known patterns
2. Only matches with >=1 keyword proceed to ML
3. ML must have confidence >=75% to approve
4. Either check fails -> REJECT with clear reason

Advantages:
+ Prevents misclassification of unclear materials
+ Clear feedback when scan cannot be processed
+ Reduces false positives in production
+ Improves overall system reliability

Next Steps:
- Implement preprocessing improvement (Indonesian stemming)
- Retrain with negative samples (optional but recommended)
- Deploy 2-skema logic in Android app
- Monitor rejection rate in production
- Collect failed scans for future training

Status: READY FOR IMPLEMENTATION
""".format(confirmed_count, total_tests, unknown_count))

print("="*80)
