@echo off
REM KitchenGuard CSM - Start ML REST API
REM This script starts the FastAPI server

echo ========================================
echo Starting KitchenGuard CSM ML REST API
echo ========================================
echo.

REM Check if models exist
if not exist "models\waste_classification\waste_classifier_model.joblib" (
    echo WARNING: Models not found in expected location
    echo Will try alternative locations...
)

echo Starting server on http://0.0.0.0:8000
echo Press Ctrl+C to stop the server
echo.

REM Start uvicorn server
cd app/api
python ml_rest_api.py

pause
