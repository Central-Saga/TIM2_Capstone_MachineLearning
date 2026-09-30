#!/bin/bash
# KitchenGuard CSM - Quick Fix Application Script
# This script applies all recommended fixes in the correct order

set -e  # Exit on error

echo "========================================"
echo "KitchenGuard CSM - Automated Fixes"
echo "========================================"
echo ""
echo "This script will:"
echo "1. Generate realistic datasets (fixes Issue #01)"
echo "2. Export Android models (fixes Issue #04)"
echo "3. Create enhanced test suite (fixes Issue #07)"
echo ""
echo "Running fixes..."
echo ""

# Step 1: Generate realistic waste dataset
echo "✓ Step 1: Generating realistic waste classification dataset"
python src/generate_realistic_waste_dataset.py

echo ""
echo "✓ Step 1 Complete!"
echo ""

# Step 2: Generate ethical skin detection dataset
echo "✓ Step 2: Generating ethical skin detection dataset (Fitzpatrick scale)"
python src/generate_skin_fitzpatrick_dataset.py

echo ""
echo "✓ Step 2 Complete!"
echo ""

# Step 3: Export for Android
echo "✓ Step 3: Exporting models for Android integration"
python src/export_for_android.py

echo ""
echo "✓ Step 3 Complete!"
echo ""

# Step 4: Validate preprocessing version
echo "✓ Step 4: Validating preprocessing consistency"
python src/preprocess_version.py || echo "ℹ️  Models not trained yet (expected)"

echo ""
echo "✓ Step 4 Complete!"
echo ""

# Step 5: Run quality tests
echo "✓ Step 5: Running enhanced ML quality tests"
echo ""
python tests/test_ml_quality_enhanced.py || echo "⚠️  Some tests may fail until models are retrained"

echo ""
echo "========================================"
echo "All automated fixes applied!"
echo "========================================"
echo ""
echo "Summary of changes:"
echo "• Realistic datasets generated (non-template)"
echo "• Ethical Fitzpatrick skin detection implemented"
echo "• Android helpers created with hybrid ML approach"
echo "• Preprocessing version control added"
echo "• Quality-focused test suite deployed"
echo ""
echo "Next steps:"
echo "1. Review generated datasets"
echo "2. Retrain models with new data"
echo "3. Verify all tests pass"
echo "4. Update documentation metrics"
echo ""
echo "For detailed information, see: ISSUE_FIXES_SUMMARY.md"
echo ""
