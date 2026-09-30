#!/usr/bin/env python
"""
Run KitchenGuard CSM ML API Server
This ensures correct working directory and paths are set
"""
import sys
import os

# Set project root as working directory
project_root = os.path.dirname(os.path.abspath(__file__))
os.chdir(project_root)
sys.path.insert(0, os.path.join(project_root, 'src'))

print(f"Starting KitchenGuard CSM ML API")
print(f"Working directory: {os.getcwd()}")
print("="*60)

import uvicorn
from app.api.ml_rest_api import app

uvicorn.run(app, host="0.0.0.0", port=8000, log_level="info")

if __name__ == "__main__":
    run()
