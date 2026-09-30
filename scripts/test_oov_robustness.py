"""
KitchenGuard CSM - OOV (Out of Vocabulary) Evaluation Test Suite
===================================================================
Test waste classification model performance on OUT-OF-VOCABULARY inputs.
These are texts that were NOT present in the training data.

This evaluates:
1. Model robustness to novel patterns
2. Fallback mechanism effectiveness  
3. Real-world accuracy (not just memorization)
4. Edge case handling capabilities
"""

import sys
from pathlib import Path
import csv
from sklearn.metrics import classification_report, precision_score, recall_score, f1_score

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from preprocess import preprocess_text
try:
    from train_improved_waste_model import load_model_pipeline
except ImportError:
    print("⚠️  Warning: train_improved_waste_model not found, using fallback")


class TestOOVRobustness:
    """Test suite for Out-of-Vocabulary scenario evaluation"""
    
    def __init__(self):
        self.results = []
        self.oov_samples = []
        
    def load_oov_dataset(self, dataset_path: Path = None):
        """Load OOV test dataset"""
        if dataset_path is None:
            dataset_path = Path(__file__).parent.parent / "data" / "oov_test_dataset.csv"
        
        if not dataset_path.exists():
            print(f"❌ OOV dataset not found: {dataset_path}")
            print("💡 Run: python scripts/generate_oov_test_dataset.py first")
            return False
        
        with open(dataset_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            self.oov_samples = list(reader)
        
        print(f"✅ Loaded {len(self.oov_samples)} OOV samples")
        return True
    
    def load_model(self):
        """Load trained model pipeline"""
        try:
            self.model, self.vectorizer, self.encoder = load_model_pipeline()
            print("✅ Model loaded successfully")
            return True
        except Exception as e:
            print(f"❌ Model loading failed: {e}")
            self.model = None
            return False
    
    def predict_single(self, text: str):
        """Predict category for a single text"""
        if self.model is None:
            return {"category": "UNKNOWN", "confidence": 0.0, "fallback": True}
        
        try:
            # Preprocess
            cleaned = preprocess_text(text)
            
            # Vectorize
            X = self.vectorizer.transform([cleaned])
            
            # Predict
            pred_idx = self.model.predict(X)[0]
            proba = self.model.predict_proba(X)[0][pred_idx]
            
            pred_label = self.encoder.inverse_transform([pred_idx])[0]
            
            return {
                "category": pred_label,
                "confidence": float(proba),
                "fallback": False
            }
            
        except Exception as e:
            return {"category": "UNKNOWN", "confidence": 0.0, "fallback": True}
    
    def evaluate_single_sample(self, sample: dict) -> dict:
        """Evaluate one OOV sample"""
        text = sample["input_text"]
        expected = sample["expected_category"]
        
        prediction = self.predict_single(text)
        
        # Calculate metrics
        correct = prediction["category"] == expected
        matched_keywords = self._count_matching_keywords(text, expected)
        
        result = {
            "sample_id": sample["sample_id"],
            "input_text": text,
            "category_type": sample["category_type"],
            "difficulty": sample["difficulty"],
            "expected_category": expected,
            "predicted_category": prediction["category"],
            "confidence": prediction["confidence"],
            "correct": correct,
            "fallback_used": prediction["fallback"],
            "matching_keywords": matched_keywords,
            "has_typo": int(sample["has_typo"]),
            "mixed_language": int(sample["has_mixed_language"]),
        }
        
        self.results.append(result)
        return result
    
    def _count_matching_keywords(self, text: str, expected_category: str) -> int:
        """Count how many keywords match between input and expected category"""
        keyword_map = {
            "CONTAMINATED": ["lantai", "floor", "contaminated", "terkontaminasi", 
                            "hair", "sewage", "pest", "rodent", "shard", "metal"],
            "SPOILED": ["jamur", "moldy", "bau", "smell", "sour", "curdled", "rotten"],
            "EXPIRED": ["expired", "kedaluwarsa", "mhd", "date", "lewat", "thawed", "melted"],
            "OVERCOOKED": ["burnt", "hangus", "gosong", "overcook", "black", "charred"],
            "PREP_WASTE": ["trimming", "cutting", "prep", "dice", "chop", "peel"],
            "SURPLUS": ["surplus", "excess", "leftover", "unserved", "remaining"],
        }
        
        text_lower = text.lower()
        keywords_present = sum(1 for kw in keyword_map.get(expected_category, []) 
                              if kw in text_lower)
        
        return keywords_present
    
    def run_evaluation(self):
        """Run full OOV evaluation"""
        if not self.oov_samples:
            print("❌ No OOV samples loaded")
            return
        
        print(f"\n🧪 Running OOV evaluation on {len(self.oov_samples)} samples...")
        print("=" * 70)
        
        passed = 0
        failed = 0
        fallbacks = 0
        
        for i, sample in enumerate(self.oov_samples, 1):
            result = self.evaluate_single_sample(sample)
            
            if result["correct"]:
                passed += 1
                status = "✅"
            else:
                failed += 1
                status = "❌"
            
            if result["fallback_used"]:
                fallbacks += 1
            
            # Print progress every 10 samples
            if i % 10 == 0 or i == len(self.oov_samples):
                print(f"{status} [{i:3d}/{len(self.oov_samples)}] {sample['input_text'][:60]}...")
                print(f"   Expected: {result['expected_category']} | Predicted: {result['predicted_category']}")
                print(f"   Confidence: {result['confidence']:.2%} | Correct: {result['correct']}")
        
        print("\n" + "=" * 70)
        print("📊 EVALUATION SUMMARY")
        print("=" * 70)
        
        accuracy = passed / len(self.oov_samples)
        fallback_rate = fallbacks / len(self.oov_samples)
        
        print(f"\nOverall Accuracy: {accuracy:.2%} ({passed}/{len(self.oov_samples)})")
        print(f"Fallback Rate: {fallback_rate:.2%} ({fallbacks}/{len(self.oov_samples)})")
        
        # Detailed metrics
        y_true = [r["expected_category"] for r in self.results]
        y_pred = [r["predicted_category"] for r in self.results]
        
        print(f"\nClassification Report:")
        print(classification_report(y_true, y_pred, zero_division=0))
        
        # Per-category breakdown
        print("\n📈 Performance by Category Type:")
        type_performance = {}
        for sample in self.oov_samples:
            cat_type = sample["category_type"]
            if cat_type not in type_performance:
                type_performance[cat_type] = {"total": 0, "correct": 0}
            type_performance[cat_type]["total"] += 1
            if sample["sample_id"] in [r["sample_id"] for r in self.results if r["correct"]]:
                type_performance[cat_type]["correct"] += 1
        
        for cat_type, stats in type_performance.items():
            cat_accuracy = stats["correct"] / stats["total"] if stats["total"] > 0 else 0
            print(f"   {cat_type}: {cat_accuracy:.2%} ({stats['correct']}/{stats['total']})")
        
        # Difficulty analysis
        print(f"\n📈 Performance by Difficulty:")
        diff_performance = {}
        for result in self.results:
            diff = result["difficulty"]
            if diff not in diff_performance:
                diff_performance[diff] = {"total": 0, "correct": 0}
            diff_performance[diff]["total"] += 1
            if result["correct"]:
                diff_performance[diff]["correct"] += 1
        
        for diff, stats in diff_performance.items():
            diff_accuracy = stats["correct"] / stats["total"] if stats["total"] > 0 else 0
            print(f"   {diff}: {diff_accuracy:.2%} ({stats['correct']}/{stats['total']})")
        
        # Typo analysis
        typo_samples = [r for r in self.results if r["has_typo"] == 1]
        if typo_samples:
            typo_accuracy = sum(1 for r in typo_samples if r["correct"]) / len(typo_samples)
            print(f"\n📈 Typo Handling: {typo_accuracy:.2%} accuracy on misspelled inputs")
        
        # Mixed language analysis
        mixed_samples = [r for r in self.results if r["mixed_language"] == 1]
        if mixed_samples:
            mixed_accuracy = sum(1 for r in mixed_samples if r["correct"]) / len(mixed_samples)
            print(f"📈 Mixed Language: {mixed_accuracy:.2%} accuracy on code-switching inputs")
        
        # Save detailed results
        self.save_results()
        
        return {
            "accuracy": accuracy,
            "fallback_rate": fallback_rate,
            "total_samples": len(self.oov_samples),
            "passed": passed,
            "failed": failed,
        }
    
    def save_results(self):
        """Save detailed evaluation results to CSV"""
        output_path = Path(__file__).parent.parent / "reports" / "oov_evaluation_results.csv"
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'w', newline='', encoding='utf-8') as f:
            fieldnames = ["sample_id", "input_text", "category_type", "difficulty",
                         "expected_category", "predicted_category", "confidence",
                         "correct", "fallback_used", "matching_keywords"]
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(self.results)
        
        print(f"\n💾 Detailed results saved to: {output_path}")


def main():
    """Main evaluation runner"""
    print("=" * 70)
    print("🔬 KITCHENGUARD CSM - OOV (OUT OF VOCABULARY) EVALUATION")
    print("=" * 70)
    print("\n🎯 Purpose: Test model performance on UNSEEN/novel inputs")
    print("   • Simulates real-world usage scenarios")
    print("   • Evaluates robustness to edge cases")
    print("   • Identifies model weaknesses and improvement areas")
    print()
    
    evaluator = TestOOVRobustness()
    
    # Step 1: Load OOV dataset
    print("[1/3] Loading OOV test dataset...")
    if not evaluator.load_oov_dataset():
        return
    
    # Step 2: Load model
    print("\n[2/3] Loading trained model...")
    if not evaluator.load_model():
        print("\n⚠️  Using fallback keyword matching instead...")
        # For now, we'll continue with manual predictions
    
    # Step 3: Run evaluation
    print("\n[3/3] Running OOV evaluation...")
    metrics = evaluator.run_evaluation()
    
    print("\n" + "=" * 70)
    print("✨ EVALUATION COMPLETE")
    print("=" * 70)
    print(f"\n📊 Final Metrics:")
    print(f"   • Overall Accuracy: {metrics['accuracy']:.2%}")
    print(f"   • Fallback Rate: {metrics['fallback_rate']:.2%}")
    print(f"   • Total Samples Tested: {metrics['total_samples']}")
    print(f"   • Passed: {metrics['passed']}")
    print(f"   • Failed: {metrics['failed']}")
    
    if metrics['accuracy'] < 0.70:
        print("\n⚠️  WARNING: OOV accuracy below 70% threshold")
        print("   Consider: expanding training data, improving preprocessing, adding more keywords")
    elif metrics['accuracy'] < 0.85:
        print("\nℹ️  Note: OOV accuracy moderate - room for improvement")
        print("   Focus on: ambiguous inputs, mixed language, and typo handling")
    else:
        print("\n✅ Good! Model shows strong generalization to unseen inputs")
    
    print("\n💡 Recommendations:")
    if metrics['fallback_rate'] > 0.30:
        print("   • Reduce fallback rate by improving keyword coverage")
    if metrics['accuracy'] < 0.80:
        print("   • Add more diverse training examples")
    print("   • Consider ensemble methods for better generalization")
    print("   • Implement active learning for hard samples")


if __name__ == "__main__":
    main()
