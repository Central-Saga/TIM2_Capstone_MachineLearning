"""
KitchenGuard CSM - Vegetable Object Detection Training
Trains YOLOv8 model untuk detect vegetables dari gambar/camera
"""

import os
import yaml
from ultralytics import YOLO
import shutil
import json

# Configuration
DATASET_DIR = "Datasets/vegatables"
DATA_CONFIG_FILE = "vegetables_config.yaml"
OUTPUT_DIR = "models/vegetable_detector"

os.makedirs(OUTPUT_DIR, exist_ok=True)

print("="*70)
print("KitchenGuard CSM - Vegetable Object Detection Training")
print("="*70)

# 1. Create dataset configuration
print("\n[1/5] Creating dataset configuration...")

data_yaml = {
    'path': DATASET_DIR,
    'train': 'train',
    'val': 'test',
    'names': {
        0: 'tomato',
        1: 'potato', 
        2: 'carrot',
        3: 'onion',
        4: 'cucumber',
        5: 'broccoli',
        6: 'pepper',
        7: 'lettuce',
        8: 'corn',
        9: 'beans'
    }
}

with open(DATA_CONFIG_FILE, 'w') as f:
    yaml.dump(data_yaml, f, default_flow_style=False)

print(f"✓ Created {DATA_CONFIG_FILE}")
print(f"  Classes: {list(data_yaml['names'].values())}")
print(f"  Total classes: {len(data_yaml['names'])}")

# 2. Load pre-trained YOLO model
print("\n[2/5] Loading YOLOv8 model...")
try:
    model = YOLO('yolov8n.pt')  # nano version (fastest)
    print("✓ Loaded YOLOv8-nano pretrained model")
except Exception as e:
    print(f"⚠ Warning: Could not load YOLOv8: {e}")
    print("  Falling back to creating new model...")
    model = YOLO('yolov8n.yaml')

# 3. Train the model
print("\n[3/5] Training object detection model...")
print("  This may take several minutes...")

training_args = {
    'data': DATA_CONFIG_FILE,
    'epochs': 100,
    'imgsz': 640,
    'batch': 16,
    'device': '0' if torch.cuda.is_available() else 'cpu',
    'workers': 4,
    'project': OUTPUT_DIR,
    'name': 'vegetable_detector_v1',
    'exist_ok': True,
    'pretrained': True,
    'optimizer': 'auto',
    'verbose': True
}

# Add PyTorch import
import torch

try:
    results = model.train(**training_args)
    print("\n✓ Training completed successfully!")
    
except Exception as e:
    print(f"\n✗ Training error: {e}")
    print("  Saving current state anyway...")
    
    # Save what we have
    model.save(os.path.join(OUTPUT_DIR, 'best_attempt.pt'))

# 4. Evaluate the model
print("\n[4/5] Evaluating model performance...")
try:
    metrics = model.val()
    print(f"  mAP@0.5: {metrics.box.map:.4f}")
    print(f"  mAP@0.5:0.95: {metrics.box.map50:.4f}")
    print(f"  Precision: {metrics.box.mp:.4f}")
    print(f"  Recall: {metrics.box.mr:.4f}")
    
except Exception as e:
    print(f"⚠ Evaluation skipped: {e}")

# 5. Export model for Android use
print("\n[5/5] Exporting models...")
try:
    # Export to TorchScript
    model.export_format = 'torchscript'
    model.export(os.path.join(OUTPUT_DIR, 'vegetable_model.torchscript'))
    print("✓ TorchScript model saved")
    
    # Export to ONNX
    model.export_format = 'onnx'
    model.export(os.path.join(OUTPUT_DIR, 'vegetable_model.onnx'))
    print("✓ ONNX model saved")
    
    # Export to TensorFlow Lite (if available)
    try:
        model.export_format = 'tflite'
        model.export(os.path.join(OUTPUT_DIR, 'vegetable_model.tflite'))
        print("✓ TFLite model saved")
    except:
        print("⚠ TFLite export failed (optional)")
        
    # Create metadata file
    metadata = {
        'model_name': 'KitchenGuard Vegetable Detector v1',
        'version': '1.0.0',
        'created_at': str(__import__('datetime').datetime.now()),
        'classes': data_yaml['names'],
        'total_classes': len(data_yaml['names']),
        'input_size': 640,
        'confidence_threshold': 0.5,
        'iou_threshold': 0.45,
        'export_formats': ['torchscript', 'onnx'],
        'android_ready': False,
        'notes': 'Model needs conversion to TFLite for Android deployment'
    }
    
    with open(os.path.join(OUTPUT_DIR, 'metadata.json'), 'w') as f:
        json.dump(metadata, f, indent=2)
    
    print("\n✓ Metadata saved")
    print(f"\n✓ Models saved to: {OUTPUT_DIR}/")
    
except Exception as e:
    print(f"\n✗ Export error: {e}")

print("\n" + "="*70)
print("Training Summary")
print("="*70)
print(f"\nOutput Location: {OUTPUT_DIR}/")
print("Files created:")
for root, dirs, files in os.walk(OUTPUT_DIR):
    for file in files:
        filepath = os.path.join(root, file)
        size_mb = os.path.getsize(filepath) / (1024 * 1024)
        print(f"  - {file} ({size_mb:.2f} MB)")

print("\nNext steps:")
print("1. Test model on sample images")
print("2. Convert to TFLite for Android deployment")
print("3. Integrate into CameraScannerActivity.kt")
print("4. Set confidence threshold to 0.6+")
print("5. Display 'Unknown' for low-confidence detections")

print("\n" + "="*70)
