"""
KitchenGuard CSM - Preprocessing Version Control Module
Ensures train/serve consistency by validating preprocessing version matches model requirements.
Prevents train/serve skew through metadata validation.
"""

import json
import os
from typing import Tuple, Dict, Any

PREPROCESSING_VERSION = "2.0.0"

def get_preprocessing_version() -> str:
    """Return current preprocessing version for compatibility checking"""
    return PREPROCESSING_VERSION


def validate_metadata(metadata: dict) -> Tuple[bool, str]:
    """Validate that model metadata matches this preprocessing version
    
    Args:
        metadata: Model metadata dictionary
        
    Returns:
        Tuple of (is_valid, message)
    """
    if not metadata:
        return False, "Metadata is empty"
    
    if "preprocessing_version" not in metadata:
        return False, f"Metadata missing preprocessing_version field"
    
    model_version = metadata.get("preprocessing_version")
    
    if model_version != PREPROCESSING_VERSION:
        return False, (
            f"Version mismatch detected!\n"
            f"Model trained with v{model_version}, but preprocessing is v{PREPROCESSING_VERSION}.\n"
            f"Please retrain the model or downgrade preprocessing to match."
        )
    
    return True, f"Preprocessing version {PREPROCESSING_VERSION} matches model requirements"


def save_model_with_metadata(model_path: str, vectorizer_path: str, 
                             encoder_path: str, metadata: Dict[str, Any]):
    """Save model artifacts with embedded preprocessing version
    
    Args:
        model_path: Path to saved model file
        vectorizer_path: Path to TF-IDF vectorizer file
        encoder_path: Path to label encoder file
        metadata: Additional metadata to save
    """
    # Add preprocessing version to metadata
    metadata["preprocessing_version"] = PREPROCESSING_VERSION
    metadata["saved_at"] = __import__('datetime').datetime.now().isoformat()
    
    # Save metadata alongside model files
    metadata_path = os.path.join(os.path.dirname(model_path), "metadata.json")
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)


def check_serve_readiness(models_dir: str = "models") -> Tuple[bool, list[str]]:
    """Check if serving environment is ready without train/serve skew
    
    Args:
        models_dir: Directory containing model artifacts
        
    Returns:
        Tuple of (is_ready, warnings_list)
    """
    import joblib
    
    issues = []
    
    # Check if waste classification directory exists
    wc_dir = os.path.join(models_dir, "waste_classification")
    if not os.path.exists(wc_dir):
        wc_dir = models_dir  # Fallback to main models dir
    
    # Load metadata
    metadata_path = os.path.join(wc_dir, "waste_classifier_metadata.json")
    if not os.path.exists(metadata_path):
        metadata_path = os.path.join(wc_dir, "model_metadata.json")
    
    if os.path.exists(metadata_path):
        with open(metadata_path, 'r', encoding='utf-8') as f:
            metadata = json.load(f)
        
        is_valid, message = validate_metadata(metadata)
        if not is_valid:
            issues.append(message)
    else:
        issues.append("No metadata file found - unable to verify preprocessing compatibility")
    
    # Check if core files exist
    required_files = [
        "waste_classifier_model.joblib",
        "tfidf_vectorizer.joblib", 
        "label_encoder.joblib"
    ]
    
    for filename in required_files:
        filepath = os.path.join(wc_dir, filename)
        if not os.path.exists(filepath):
            issues.append(f"Missing required artifact: {filename}")
    
    return len(issues) == 0, issues


if __name__ == "__main__":
    print("Preprocessing Version Check Utility")
    print("=" * 50)
    print(f"Current Preprocessing Version: {get_preprocessing_version()}")
    print()
    
    is_ready, issues = check_serve_readiness()
    
    if is_ready:
        print("✅ Serving environment is ready!")
    else:
        print("❌ Serving environment has issues:")
        for issue in issues:
            print(f"   • {issue}")
        print()
        print("Please fix these issues before deploying.")
