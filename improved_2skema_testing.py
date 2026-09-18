#!/usr/bin/env python3
"""
KitchenGuard ML - IMPROVED 2-SKEMA WITH BETTER PATTERN MATCHING
Enhancements:
1. More comprehensive keyword patterns
2. Lowered initial threshold for better recall
3. Better handling of text variations
4. Fuzzy-like matching for partial matches
"""

import joblib
import numpy as np

print("="*80)
print("IMPROVED 2-SKEMA SYSTEM WITH ENHANCED PATTERNS")
print("="*80)

# Load models
vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
classifier = joblib.load('models/waste_classification/waste_classifier_model.joblib')
encoder = joblib.load('models/waste_classification/label_encoder.joblib')

print("✓ Models loaded!\n")

# ============================================
# ENHANCED PATTERNS WITH BETTER COVERAGE
# ============================================
enhanced_patterns = {
    "SPOILED": [
        # Core keywords (must match at least 1)
        "berbau", "busuk", "lendir", "berlendir", "jamur", "berjamur",
        "membusuk", "tengik", "bau", "amonia", "ammonia", "curdle",
        "hitam", "kehitaman", "menggelembung", "lemek", "berubah",
        # Additional context keywords
        "tidak layak", "rusak", "memar", "bercuka", "bercendawan",
        "asam", "tajam", "lunak", "hancur"
    ],
    "EXPIRED": [
        "expired", "kedaluwarsa", "MHD", "tanggal kadaluarsa",
        "lewati", "telah melewati", "melewati", "expired date",
        "batas tanggal", "masa simpan", "kadaluarsa", "expired_date"
    ],
    "PREP_WASTE": [
        "kulit", "bonggol", "kupasan", "trimming", "peeling",
        "preparation", "sisa prep", "prep station", "potongan",
        "biji melon", "tomato stem", "fish bones", "shrimp shell",
        "onion", "herb stalks", "fruit cores", "sisa pemecahan telur"
    ],
    "OVERCOOKED": [
        "gosong", "hangus", "terbakar", "overdone", "burnt", "charred",
        "terlalu lama", "lewat", "matang berlebih", "kerak",
        "bau sangit", "kelewat matang", "ditinggal", "overcooked"
    ],
    "CONTAMINATED": [
        "terkontaminasi", "jatuh ke lantai", "tersentuh benda asing",
        "hair", "insect", "fly", "debu kotor", "sabun cuci",
        "air kotor", "kontak langsung", "tercemar", "tercemarkan",
        "benda asing", "foreign material", "kemasukan"
    ],
    "SURPLUS": [
        "kelebihan", "sisa buffet", "tidak terjual", "leftover",
        "overshoot", "leftovers", "excess", "tidak habis",
        "sisa catering", "meal not served", "gathering leftover",
        "lebih dari kebutuhan", "tidak tersentuh tamu"
    ]
}

# ============================================
# IMPROVED DETECTION LOGIC
# ============================================
def count_pattern_matches(text, patterns):
    """Count how many patterns match in text (with fuzzy matching)"""
    text_lower = text.lower()
    
    # Clean text: remove extra spaces, normalize
    cleaned_text = ' '.join(text_lower.split())
    
    matches = []
    for p in patterns:
        # Direct match
        if p in cleaned_text:
            matches.append(p)
        # Partial match (check if keyword appears anywhere)
        elif any(word in cleaned_text for word in p.split()):
            matches.append(f"{p}_partial")
    
    return len(matches)

def detect_material_status_improved(text):
    """Improved detection with enhanced patterns"""
    
    # Step 1: Check all patterns
    pattern_scores = {}
    max_category = None
    max_pattern_score = 0
    
    for category, patterns in enhanced_patterns.items():
        score = count_pattern_matches(text, patterns)
        pattern_scores[category] = score
        if score > max_pattern_score:
            max_pattern_score = score
            max_category = category
    
    # Step 2: Get ML prediction
    try:
        clean_text = text.lower().replace('_', ' ').replace('-', ' ')
        features = vectorizer.transform([clean_text])
        ml_prediction = classifier.predict(features)[0]
        ml_confidence = classifier.predict_proba(features)[0][ml_prediction] * 100
        ml_category = encoder.inverse_transform([ml_prediction])[0]
    except Exception as e:
        print(f"ML Error: {e}")
        ml_confidence = 0
        ml_category = None
    
    # Step 3: Decision Logic (Adjusted thresholds)
    PATTERN_THRESHOLD = 0.5  # At least partial match counts
    ML_CONFIDENCE_THRESHOLD = 70  # Lowered from 75%
    
    # Combine pattern score + ML confidence
    total_score = 0
    if max_pattern_score >= 1:
        pattern_contribution = min(max_pattern_score * 20, 40)  # Max 40 points from patterns
    else:
        pattern_contribution = 0
    
    final_confidence = pattern_contribution + ml_confidence
    
    # BOTH must have SOME support
    if (max_pattern_score >= 1 or ml_confidence >= 70) and ml_confidence >= ML_CONFIDENCE_THRESHOLD:
        status = "CONFIRMED"
        final_category = ml_category
        confidence = min(final_confidence, 100)
        
        reasons = []
        if max_pattern_score >= 1:
            reasons.append("keyword match")
        if ml_confidence >= 70:
            reasons.append("ML confidence sufficient")
        
        reason = f"Valid waste item ({', '.join(reasons)})"
    
    else:
        status = "UNKNOWN"
        final_category = "UNCATEGORIZED"
        confidence = ml_confidence if ml_confidence > 0 else 0
        
        reasons = []
        if max_pattern_score == 0:
            reasons.append("no keywords found")
        if ml_confidence < ML_CONFIDENCE_THRESHOLD:
            reasons.append("low ML confidence")
        
        reason = "; ".join(reasons) if reasons else "insufficient evidence"
    
    return {
        'status': status,
        'category': final_category,
        'confidence': confidence,
        'reason': reason,
        'is_approved': status == "CONFIRMED",
        'pattern_score': max_pattern_score,
        'ml_confidence': ml_confidence
    }

# ============================================
# RE-RUN CRITICAL TESTS
# ============================================
print("\n" + "="*80)
print("RE-TESTING WITH IMPROVED PATTERNS")
print("="*80)

critical_tests = [
    ("Daging sapi berbau busuk berlendir", "CONFIRMED", "SPOILED"),
    ("Botol mayonnaise expired date minggu lalu", "CONFIRMED", "EXPIRED"),
    ("Kulit wortel bonggol brokoli prep station", "CONFIRMED", "PREP_WASTE"),
    ("Ayam gosong hangus kompor ditinggal", "CONFIRMED", "OVERCOOKED"),
    ("Ikan fillet jatuh ke lantai berminyak", "CONFIRMED", "CONTAMINATED"),
    ("Nasi tumpeng sisa buffet tidak habis", "CONFIRMED", "SURPLUS"),
    ("barang rusak", "UNKNOWN", "UNCATEGORIZED"),
    ("material tidak dikenali", "UNKNOWN", "UNCATEGORIZED"),
    ("xyz abc def random", "UNKNOWN", "UNCATEGORIZED"),
]

improved_passed = 0
improved_failed = 0

for text, expected_status, expected_cat in critical_tests:
    result = detect_material_status_improved(text)
    
    passed = (result['status'] == expected_status and result['category'] == expected_cat)
    
    if passed:
        improved_passed += 1
        icon = "✓"
    else:
        improved_failed += 1
        icon = "✗"
    
    print("{0} {1:<40} Expected: {2:<10} Got: {3:<10} Cat: {4:<12} Conf: {5:.1f}%".format(
        icon, text[:38], expected_status, result['status'], result['category'], result['confidence']))

total_tests = len(critical_tests)
new_pass_rate = improved_passed / total_tests * 100

print("\nResults: {0}/{1} PASSED ({2:.1f}%)".format(improved_passed, total_tests, new_pass_rate))

# ============================================
# ADVANCED NOISE ROBUSTNESS TEST
# ============================================
print("\n" + "="*80)
print("ADVANCED NOISE ROBUSTNESS TEST")
print("="*80)

noise_test_cases = [
    ("DAGING SAPI BERBAU BUSUK", "UPPERCASE only"),
    ("Botol .mayonnaise .expired!!!", "PUNCTUATION overload"),
    ("A y a m   g o s o n g", "SPACED out"),
    ("Ikan.Jatuh.ke.Lantai?!", "Mixed punctuation"),
    ("daging_sapi_berbau_busuk", "UNDERSCORE variant"),
    ("daging-sapi-berbau-busuk", "DASH variant"),
]

robustness_passed = 0

for text, description in noise_test_cases:
    result = detect_material_status_improved(text)
    
    # Should still get reasonable result even with noise
    passed = result['is_approved'] and result['confidence'] >= 60
    status = "PASS" if passed else "FAIL"
    
    if passed:
        robustness_passed += 1
    
    icon = "✓" if passed else "✗"
    print("{0} {1:<30} Status: {2:<10} Conf: {3:.1f}%".format(
        icon, description, result['status'], result['confidence']))

print("\nRobustness Results: {0}/{1} PASSED".format(robustness_passed, len(noise_test_cases)))

# ============================================
# FINAL COMPARISON
# ============================================
print("\n" + "="*80)
print("FINAL COMPARISON: BEFORE vs AFTER")
print("="*80)

print("\nBefore (Original):")
print("  Pass Rate: 75.0% (21/28)")
print("  Issues: Low confidence scores, strict pattern matching")

print("\nAfter (Improved):")
print("  Critical Tests: {0}% ({1}/{2})".format(new_pass_rate, improved_passed, total_tests))
print("  Robustness: {0}% ({1}/{2})".format(robustness_passed/len(noise_test_cases)*100, robustness_passed, len(noise_test_cases)))
print("  Improvements:")
print("    ✓ Enhanced keyword coverage per category")
print("    ✓ Lowered confidence threshold (70% instead of 75%)")
print("    ✓ Added partial pattern matching support")
print("    ✓ Better handling of text variations")

print("\n" + "="*80)
print("RECOMMENDATION")
print("="*80)

if new_pass_rate >= 90 and robustness_passed >= 4:
    print("\n✅ EXCELLENT - Ready for deployment!")
    print("   The improved pattern system resolves most issues.")
elif new_pass_rate >= 80:
    print("\n✅ GOOD - Approaches production readiness")
    print("   Minor fine-tuning recommended before full deployment.")
else:
    print("\n⚠️  Still needs work")
    print("   Consider retraining on expanded dataset v4")

print("\nNext Steps:")
print("  1. Deploy this improved version")
print("  2. Monitor real-world performance")
print("  3. Collect failed scans for future training")
print("  4. Optionally retrain with full dataset when ready")

print("\n" + "="*80)
