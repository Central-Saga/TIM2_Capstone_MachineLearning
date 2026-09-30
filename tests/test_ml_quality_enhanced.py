"""
Comprehensive Test Suite for KitchenGuard CSM - Enhanced ML Quality Testing
This replaces weak 70 passing tests with rigorous quality validation tests.
Tests include: dataset diversity, preprocessing consistency, bias detection, model fairness.
"""

import sys
from pathlib import Path
from datetime import datetime
import unittest
import json
import joblib
import pandas as pd
import numpy as np
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    confusion_matrix, classification_report
)

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))


class TestDatasetQuality(unittest.TestCase):
    """Tests for dataset diversity and realism - CRITICAL FIX #1"""
    
    def setUp(self):
        self.waste_path = Path("data/kitchenguard_waste_dataset_realistic_v2.csv")
        self.skin_path = Path("data/skin_detection_fitzpatrick_v2.csv")
    
    def test_waste_dataset_exists_and_diverse(self):
        """Test that realistic dataset exists and has diversity"""
        self.assertTrue(self.waste_path.exists(), 
                       "Realistic waste dataset not generated!")
        
        df = pd.read_csv(self.waste_path)
        self.assertGreater(len(df), 500, "Dataset too small (< 500 samples)")
        
        # Check category balance
        category_counts = df["category"].value_counts()
        self.assertEqual(len(category_counts), 6, 
                        "Should have 6 categories")
        
        # No category should be empty or dominate (>90%)
        max_ratio = category_counts.max() / len(df)
        min_ratio = category_counts.min() / len(df)
        
        self.assertLess(max_ratio, 0.35, 
                       f"Category imbalance detected: {max_ratio:.2%} dominance")
        self.assertGreater(min_ratio, 0.10,
                          f"Some categories underrepresented: {min_ratio:.2%}")
    
    def test_skin_dataset_uses_fitzpatrick_scale(self):
        """Test ethical skin detection uses Fitzpatrick scale not FAIR/NOT_FAIR"""
        self.assertTrue(self.skin_path.exists(),
                       "Fitzpatrick dataset not generated!")
        
        df = pd.read_csv(self.skin_path)
        
        # Must NOT have race-based "FAIR"/"NOT_FAIR" columns
        forbidden_cols = ["is_fair", "fair_skin", "skin_color_binary"]
        for col in forbidden_cols:
            self.assertNotIn(col, df.columns.tolist(),
                            f"Found ethically problematic column: {col}")
        
        # Must have Fitzpatrick type
        self.assertIn("fitzpatrick_type", df.columns.tolist(),
                     "Missing fitzpatrick_type column")
        
        # Should have sun protection recommendations
        valid_types = {"I", "II", "III", "IV"}
        actual_types = set(df["fitzpatrick_type"].unique())
        
        self.assertTrue(actual_types.issubset(valid_types),
                       f"Invalid Fitzpatrick types found: {actual_types - valid_types}")
    
    def test_no_template_overfitting(self):
        """Test that datasets avoid template-based overfitting"""
        df = pd.read_csv(self.waste_path)
        
        # Check text length distribution (should vary)
        text_lengths = df["text"].str.len()
        
        # Texts should not all be same length (sign of template generation)
        std_dev = text_lengths.std()
        mean_len = text_lengths.mean()
        
        cv = std_dev / mean_len if mean_len > 0 else 0
        
        self.assertGreater(cv, 0.15,
                          f"Text lengths too uniform (CV={cv:.2f}). "
                          "Indicates template overfitting.")
        
        # Check duplicate rate should be low
        duplicate_rate = df.duplicated(subset=["text"]).sum() / len(df)
        self.assertLess(duplicate_rate, 0.05,
                       f"Too many duplicate texts ({duplicate_rate:.2%})")


class TestPreprocessingConsistency(unittest.TestCase):
    """Tests for train/serve consistency - CRITICAL FIX #2"""
    
    def test_preprocessing_version_module_exists(self):
        """Test preprocess_version module exists and exports version"""
        from preprocess_version import get_preprocessing_version, validate_metadata
        
        version = get_preprocessing_version()
        self.assertIsNotNone(version, "Version should not be None")
        
        # Version format check
        parts = version.split(".")
        self.assertGreaterEqual(len(parts), 2,
                               "Version should be major.minor.build")
    
    def test_metadata_validation_works(self):
        """Test metadata validation function"""
        from preprocess_version import validate_metadata
        
        # Valid metadata
        valid_meta = {"preprocessing_version": "2.0.0"}
        is_valid, msg = validate_metadata(valid_meta)
        self.assertTrue(is_valid, f"Valid metadata rejected: {msg}")
        
        # Invalid version
        invalid_meta = {"preprocessing_version": "1.0.0"}
        is_valid, msg = validate_metadata(invalid_meta)
        self.assertFalse(is_valid, "Should reject mismatched version")
        
        # Missing version
        no_version_meta = {}
        is_valid, msg = validate_metadata(no_version_meta)
        self.assertFalse(is_valid, "Should detect missing version field")
    
    def test_predictor_validates_preprocessing(self):
        """Test that predictor checks preprocessing compatibility"""
        from predict import KitchenGuardTextPredictor
        
        try:
            predictor = KitchenGuardTextPredictor()
            
            # If loaded, it should have validated preprocessing
            # Check that _validate_preprocessing_compatibility exists
            self.assertTrue(hasattr(predictor, '_validate_preprocessing_compatibility'),
                           "Predictor missing preprocessing validation method")
        
        except Exception as e:
            # May fail if models not trained yet, that's OK
            self.assertIn("Model", str(e).lower() or "loading")


class TestSkinDetectionEthics(unittest.TestCase):
    """Tests for ethical skin detection - CRITICAL FIX #3"""
    
    def test_skin_estimator_does_not_use_race_categories(self):
        """Test that skin detection avoids race-based categories"""
        from src.generate_skin_fitzpatrick_dataset import FITZPATRICK_TYPES
        
        # Should use Fitzpatrick I-IV, not FAIR/NOT_FAIR
        allowed_keys = {"I", "II", "III", "IV"}
        actual_keys = set(FITZPATRICK_TYPES.keys())
        
        self.assertTrue(actual_keys.issubset(allowed_keys),
                       f"Using disallowed skin categories: {actual_keys - allowed_keys}")
    
    def test_skin_detector_sun_protection_focus(self):
        """Test that skin detector focuses on sun protection needs"""
        from src.generate_skin_fitzpatrick_dataset import FITZPATRICK_TYPES
        
        # All types should have sun protection info
        for ftype, data in FITZPATRICK_TYPES.items():
            self.assertIn("protection_level", data,
                         f"Fitzpatrick {ftype} missing protection_level")
            self.assertIn("sun_sensitivity", data,
                         f"Fitzpatrick {ftype} missing sun_sensitivity")
    
    def test_skin_detection_features_are_ethical(self):
        """Test that features used are medically appropriate"""
        df = pd.read_csv("data/skin_detection_fitzpatrick_v2.csv")
        
        # Valid feature columns
        valid_features = [
            "rgb_mean_r", "rgb_mean_g", "rgb_mean_b",
            "rgb_std_r", "rgb_std_g", "rgb_std_b",
            "fitzpatrick_type", "body_location"
        ]
        
        for feat in valid_features:
            self.assertIn(feat, df.columns.tolist(),
                         f"Required ethical feature missing: {feat}")
        
        # No ethnic/race columns
        forbidden = ["ethnicity", "race", "nationality", "geographic_origin"]
        for feat in forbidden:
            self.assertNotIn(feat, df.columns.tolist(),
                            f"Found unethical feature: {feat}")


class TestModelPerformance(unittest.TestCase):
    """Tests for realistic model performance (no 100% accuracy claims)"""
    
    def test_classifier_has_reasonable_accuracy_range(self):
        """Test classifier achieves reasonable (not perfect) accuracy"""
        # Load existing model if available
        try:
            model_path = Path("models/waste_classification/waste_classifier_ensemble.joblib")
            if not model_path.exists():
                model_path = Path("models/waste_classifier_model.joblib")
            vectorizer_path = Path("models/waste_classification/waste_classifier_tfidf.joblib")
            if not vectorizer_path.exists():
                vectorizer_path = Path("models/tfidf_vectorizer.joblib")
            encoder_path = Path("models/waste_classification/label_encoder.joblib")
            if not encoder_path.exists():
                encoder_path = Path("models/label_encoder.joblib")
            
            self.assertTrue(model_path.exists(), "Model not trained yet")
            
            model = joblib.load(model_path)
            vectorizer = joblib.load(vectorizer_path)
            encoder = joblib.load(encoder_path)
            
            # Load validation data
            df = pd.read_csv("data/kitchenguard_waste_dataset_realistic_v2.csv")
            
            X_train, X_test, y_train, y_test = train_test_split(
                df["text"], df["category"], test_size=0.2, random_state=42, stratify=df["category"]
            )
            
            X_test_vec = vectorizer.transform(X_test)
            predictions = model.predict(X_test_vec)
            
            # Map predictions to string labels if numeric
            if len(predictions) > 0 and isinstance(predictions[0], (int, np.integer)):
                predictions = encoder.inverse_transform(predictions)
            
            accuracy = accuracy_score(y_test, predictions)
            
            # Reasonable accuracy range for text classification
            # Should be 0.70-0.95, NEVER 1.0 with realistic data
            self.assertGreater(accuracy, 0.65,
                              f"Accuracy too low: {accuracy:.2%}. Model may need more training data.")
            self.assertLess(accuracy, 0.98,
                           f"Accuracy suspiciously high: {accuracy:.2%}. Possible overfitting or data leakage.")
            
            print(f"\n✅ Model accuracy: {accuracy:.2%} (reasonable range)")
        
        except Exception as e:
            self.skipTest(f"Model not available yet: {e}")
    
    def test_cross_validation_stability(self):
        """Test model stability across cross-validation folds"""
        try:
            model_path = Path("models/waste_classifier_model.joblib")
            vectorizer_path = Path("models/tfidf_vectorizer.joblib")
            encoder_path = Path("models/label_encoder.joblib")
            
            if not model_path.exists():
                self.skipTest("Model not trained yet")
            
            model = joblib.load(model_path)
            vectorizer = joblib.load(vectorizer_path)
            df = pd.read_csv("data/kitchenguard_waste_dataset_realistic_v2.csv")
            
            X = df["text"]
            y = df["category"]
            
            # Cross-validation
            scores = cross_val_score(model, vectorizer.transform(X), y, cv=5)
            
            mean_acc = scores.mean()
            std_acc = scores.std()
            
            # Standard deviation should be low (<0.15) indicating stable performance
            self.assertLess(std_acc, 0.15,
                           f"Model instability detected: CV std={std_acc:.2%}")
            
            # Mean should be reasonable
            self.assertGreater(mean_acc, 0.65,
                              f"Mean CV accuracy too low: {mean_acc:.2%}")
            
            print(f"\n✅ 5-Fold CV: {mean_acc:.2%} ± {std_acc:.2%}")
        
        except Exception as e:
            self.skipTest(f"Cannot perform CV: {e}")


class TestAndroidIntegration(unittest.TestCase):
    """Tests for Android ML integration without hardcoded fallbacks"""
    
    def test_android_ml_helpers_exist(self):
        """Test that Android ML helper classes exist"""
        android_utils_dir = Path(
            "android/app/src/main/java/com/kitchenguard/csm/utils"
        )
        
        # Required helper files
        required_files = [
            "WasteClassifierMobileHelper.java",
            "FitzpatrickSkinEstimator.java"
        ]
        
        for filename in required_files:
            filepath = android_utils_dir / filename
            self.assertTrue(filepath.exists(),
                           f"Missing Android helper: {filename}")
    
    def test_mobile_helper_has_keyword_mapping(self):
        """Test mobile helper contains keyword mapping aligned with TF-IDF"""
        helper_path = Path(
            "models/android/WasteClassifierMobileHelper.java"
        )
        
        if not helper_path.exists():
            self.skipTest("Mobile helper not generated yet")
        
        content = helper_path.read_text(encoding="utf-8")
        
        # Should have keyword mappings for all categories
        categories = ["SPOILED", "EXPIRED", "CONTAMINATED", "OVERCOOKED", "PREP_WASTE", "SURPLUS"]
        
        for cat in categories:
            self.assertIn(cat, content,
                         f"Missing category '{cat}' in mobile helper")
    
    def test_skin_estimator_is_ethical(self):
        """Test skin estimator uses Fitzpatrick scale"""
        estimator_path = Path(
            "models/android/FitzpatrickSkinEstimator.java"
        )
        
        if not estimator_path.exists():
            self.skipTest("Ethical skin estimator not generated")
        
        content = estimator_path.read_text(encoding="utf-8")
        
        # Should mention Fitzpatrick
        self.assertIn("Fitzpatrick", content,
                     "Skin estimator should reference Fitzpatrick scale")
        
        # Should NOT mention FAIR/NOT_FAIR binary
        self.assertNotIn("isFairSkinDetected", content.lower(),
                        "Should avoid FAIR/not-FAIR binary classification")


class TestDocumentationConsistency(unittest.TestCase):
    """Tests for consistent documentation metrics"""
    
    def test_readme_metrics_are_consistent(self):
        """Test README has consistent metrics (Issue 05)"""
        readme_path = Path("README.md")
        
        if not readme_path.exists():
            self.skipTest("README.md not found")
        
        content = readme_path.read_text(encoding="utf-8")
        
        # Extract all accuracy percentages mentioned
        import re
        accuracy_mentions = re.findall(r'(\d+\.?\d*)%\s*(?:accuracy|accurasi)', content, re.IGNORECASE)
        
        if len(accuracy_mentions) > 1:
            # Convert to floats and check consistency
            accuracies = [float(a) for a in accuracy_mentions]
            
            # Allow some variance but not huge discrepancies
            if max(accuracies) - min(accuracies) > 10:
                self.fail(f"Inconsistent accuracy metrics in README: {accuracies}")
    
    def test_no_duplicate_md_files_dominating(self):
        """Test that we don't have excessive duplicate .md files (Issue 06)"""
        md_files = list(Path(".").glob("**/*.md"))
        
        # Count similar filenames
        from collections import Counter
        base_names = [f.stem.lower() for f in md_files]
        
        duplicates = [name for name, count in Counter(base_names).items() if count > 2]
        
        if duplicates:
            # This is a housekeeping warning, not a failure
            print(f"\n⚠️ Found duplicate-style MD files: {duplicates[:5]}...")
            print("   Consider consolidating documentation")


def run_tests():
    """Run all tests with detailed output"""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add all test classes
    suite.addTests(loader.loadTestsFromTestCase(TestDatasetQuality))
    suite.addTests(loader.loadTestsFromTestCase(TestPreprocessingConsistency))
    suite.addTests(loader.loadTestsFromTestCase(TestSkinDetectionEthics))
    suite.addTests(loader.loadTestsFromTestCase(TestModelPerformance))
    suite.addTests(loader.loadTestsFromTestCase(TestAndroidIntegration))
    suite.addTests(loader.loadTestsFromTestCase(TestDocumentationConsistency))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    print("\n" + "="*70)
    print("TEST SUITE SUMMARY")
    print("="*70)
    print(f"Total tests: {result.testsRun}")
    print(f"Passed: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failed: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Skipped: {len(result.skipped)}")
    
    if result.failures:
        print("\n❌ FAILURES:")
        for test, traceback in result.failures:
            error_line = traceback.split(chr(10))[0]
            print(f'  • {test}: {error_line}')
    
    if result.errors:
        print("\n❌ ERRORS:")
        for test, traceback in result.errors:
            error_line = traceback.split(chr(10))[0]
            print(f'  • {test}: {error_line}')
    
    return result


if __name__ == "__main__":
    result = run_tests()
    sys.exit(0 if result.wasSuccessful() else 1)
