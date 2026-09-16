@echo off
echo ========================================
echo KitchenGuard CSM v3.0 - Quick Start
echo ========================================
echo.
echo Starting FastAPI server...
echo Server will be available at: http://localhost:8000
echo API Docs: http://localhost:8000/docs
echo.
echo Press Ctrl+C to stop the server
echo.

python -m uvicorn app:app --host 127.0.0.1 --port 8000 --reload

pause
