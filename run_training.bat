@echo off
REM KitchenGuard CSM - Automated Training Pipeline
REM Run this script to train all ML models

echo ============================================================
echo    KitchenGuard CSM - Machine Learning Training Pipeline
echo ============================================================
echo.

cd /d "%~dp0src"

echo [1/4] Generating datasets...
echo ----------------------------------------
python generate_skin_dataset.py
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Dataset generation failed!
    pause
    exit /b 1
)
echo Datasets generated successfully!
echo.

echo [2/4] Training skin detection model...
echo ----------------------------------------
python train_skin_model.py
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Skin model training failed!
    pause
    exit /b 1
)
echo Skin detection model trained successfully!
echo.

echo [3/4] Training waste classification model...
echo ----------------------------------------
python train_improved_waste_model.py
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Waste classifier training failed!
    pause
    exit /b 1
)
echo Waste classification model trained successfully!
echo.

echo [4/4] Verifying outputs...
echo ----------------------------------------
if exist "..\models\skin_type_classifier.joblib" (
    echo ✓ Skin type classifier ready
) else (
    echo ✗ Skin type classifier missing
)

if exist "..\models\waste_classifier_ensemble.joblib" (
    echo ✓ Waste classifier ensemble ready
) else (
    echo ✗ Waste classifier missing
)

if exist "..\models\Android_SkinDetector.java" (
    echo ✓ Android Java helper created
) else (
    echo ✗ Android helper missing
)

echo.
echo ============================================================
echo    Training Complete!
echo ============================================================
echo.
echo Models saved to: ..\models\
echo Reports saved to: ..\reports\
echo Dataset saved to: ..\data\
echo.
echo Next steps:
echo 1. Review accuracy metrics above
echo 2. Copy models to android/app/models/
echo 3. Convert to TensorFlow Lite (optional)
echo 4. Integrate into MainActivity.kt
echo.
echo Press any key to exit...
pause >nul
