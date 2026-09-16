"""
KitchenGuard CSM - Skin Tone Detection Dataset Generator
Membuat dataset untuk klasifikasi warna kulit orang putih (Fair Skin)
Digunakan untuk validasi hygiene dan safety protocol di dapur
"""

import os
import random
import numpy as np
import pandas as pd
from PIL import Image
import cv2

random.seed(42)
np.random.seed(42)

# Fitpatrick Scale untuk kulit putih (Type I-III)
FAIR_SKIN_RANGES = {
    "FAIR_1": {"min_rgb": [240, 220, 200], "max_rgb": [255, 235, 215], "description": "Sangat terang, mudah terbakar"},
    "FAIR_2": {"min_rgb": [230, 210, 190], "max_rgb": [245, 225, 210], "description": "Terang, terbakar"},
    "FAIR_3": {"min_rgb": [220, 200, 180], "max_rgb": [235, 215, 200], "description": "Terang, sedikit tan"},
}

def generate_skin_pixel():
    """Generate pixel data untuk fair skin dengan variasi realistis"""
    skin_type = random.choice(list(FAIR_SKIN_RANGES.keys()))
    range_data = FAIR_SKIN_RANGES[skin_type]
    
    r = random.randint(range_data["min_rgb"][0], range_data["max_rgb"][0])
    g = random.randint(range_data["min_rgb"][1], range_data["max_rgb"][1])
    b = random.randint(range_data["min_rgb"][2], range_data["max_rgb"][2])
    
    return np.array([r, g, b], dtype=np.uint8), skin_type

    def generate_synthetic_face_image(width=64, height=64):
    """Generate gambar wajah sintetis untuk training"""
    img = np.zeros((height, width, 3), dtype=np.uint8)
    
    # Buat oval wajah dengan skin tone
    center = (width // 2, height // 2)
    axes = (width // 3, height // 2.5)
    
    color, skin_type = generate_skin_pixel()
    
    # Draw ellipse face shape
    cv2.ellipse(img, center, axes, 0, 0, 360, tuple(color.astype(int)), -1)
    
    # Tambahkan noise/variasi natural
    noise = np.random.randint(-10, 10, img.shape, dtype=np.int16)
    img = np.clip(img.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    
    return img, skin_type

def generate_hand_image(width=64, height=64):
    """Generate gambar tangan sintetis untuk training"""
    img = np.zeros((height, width, 3), dtype=np.uint8)
    
    color, skin_type = generate_skin_pixel()
    
    # Generate multiple circles untuk jari
    positions = [
        (width//4, height//3),
        (width//2, height//3),
        (width*3//4, height//3),
        (width//4, height//2),
        (width//2, height//2),
        (width*3//4, height//2),
    ]
    
    for pos in positions:
        radius = random.randint(5, 10)
        cv2.circle(img, pos, radius, tuple(color.astype(int)), -1)
    
    # Add palm area
    cv2.circle(img, (width//2, height//1.5), 15, tuple(color.astype(int)), -1)
    
    # Add noise
    noise = np.random.randint(-8, 8, img.shape, dtype=np.int16)
    img = np.clip(img.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    
    return img, skin_type

def generate_dataset_samples(num_samples_per_category=500):
    """Generate samples untuk skin detection"""
    rows = []
    
    categories = ["HAND", "FACE"]
    
    for category in categories:
        for i in range(num_samples_per_category):
            if category == "HAND":
                img, skin_type = generate_hand_image()
            else:
                img, skin_type = generate_synthetic_face_image()
            
            # Encode sebagai base64 atau RGB values
            rgb_mean = np.mean(img, axis=(0, 1))
            rgb_std = np.std(img, axis=(0, 1))
            
            sample = {
                "image_id": f"SKIN_{category}_{i+1:04d}",
                "category": category,
                "skin_type": skin_type,
                "rgb_mean_r": float(rgb_mean[0]),
                "rgb_mean_g": float(rgb_mean[1]),
                "rgb_mean_b": float(rgb_mean[2]),
                "rgb_std_r": float(rgb_std[0]),
                "rgb_std_g": float(rgb_std[1]),
                "rgb_std_b": float(rgb_std[2]),
                "lighting_condition": random.choice(["NORMAL", "BRIGHT", "DIM"]),
                "description": FAIR_SKIN_RANGES[skin_type]["description"],
                "hygiene_note": random.choice([
                    "Clean visible skin detected",
                    "Hand washing required",
                    "Gloves must be worn",
                    "Proper hygenic practice observed"
                ])
            }
            rows.append(sample)
    
    df = pd.DataFrame(rows)
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    
    return df

def generate_waste_quality_dataset(samples_per_class=300):
    """Expand waste quality dataset dengan variasi lebih banyak"""
    rows = []
    
    categories = {
        "SPOILED": {
            "subjects": ["Ayam segar", "Daging sapi cincang", "Ikan salmon", "Telur ayam", "Tahu segar"],
            "conditions": ["berbau busuk tidak sedap", "berlendir keputihan", "berjamur hijau kebiruan", "berlendir kekuningan", "berubah warna kehijauan"],
            "locations": ["di chiller utama", "dalam freezer", "di rak pantry", "pada counter prep station", "di dry storage"]
        },
        "EXPIRED": {
            "subjects": ["Susu cream", "Mayonaise", "Ketchup", "Saus tiram", "Minyak zaitun", "Keju cheddar"],
            "conditions": ["telah lewat expired date 3 hari", "expired date hari ini", "terlewat 1 minggu dari expiry", "sudah melewati MHD 2 minggu", "expired date terlampaui"],
            "locations": ["di refrigerated section", "di dry goods shelf", "in cooler cabinet", "pada cold storage rack"]
        },
        "PREP_WASTE": {
            "subjects": ["Potongan wortel", "Batang seledri", "Kulit kentang", "Ranting parsley", "Batang asparagus", "Ujung daun bawang"],
            "conditions": ["terbuang saat pemotongan", "sisa trimming yang banyak", "bagian tak terpakai dari prep", "over-trimmed during prep", "excessive peeling waste"],
            "locations": ["di prep station", "pada cutting board", "di sink area", "during vegetable prep"]
        },
        "OVERCOOKED": {
            "subjects": ["Steak sapi ribeye", "Ikan cod fillet", "Sayuran brokoli", "Kentang goreng", "Ayam grilled", "Rice pilaf"],
            "conditions": ["terlalu gosong kecoklatan", "kering berlebihan terlalu hard", "hangus kehitaman", "keras sekali terlalu overdone", "kekuatan tekstur berubah keras"],
            "locations": ["dari kompor main stove", "setelah oven baking", "dari deep fryer", "grilled on BBQ"]
        },
        "CONTAMINATED": {
            "subjects": ["Salad Caesar", "Soup tomato", "Noodle dish", "Rice bowl", "Sandwich club"],
            "conditions": ["terkontaminasi hair异物", "tersentuh oleh hands bare", "jatuh ke lantai langsung", "tercampur cleaning chemical spray", "tersentuh paper towel kotor"],
            "locations": ["di plating area", "pada service counter", "during food assembly", "at garnish station"]
        },
        "SURPLUS": {
            "subjects": ["Portion rice", "Grilled chicken breast", "Steamed broccoli", "Fresh fruit salad", "Chicken stock"],
            "conditions": ["tidak terjual hari ini", "over-prepared untuk service", "leftover setelah shift", "tidak tersentuh customer", "excess production batch"],
            "locations": ["in holding warmer", "prepped but unserved", "end of dinner service", "after lunch rush"]
        }
    }
    
    action_phrases = [
        "wajib segera dibuang sesuai SOP",
        "harus dibuatkan waste log entry",
        "direject oleh kitchen manager",
        "noted dalam daily waste report",
        "disposal required immediately"
    ]
    
    connector_phrases = [
        "", "catatan chef:", "laporan staff shift:", "inspeksi menemukan", "kondisi aktual:"
    ]
    
    for category, content in categories.items():
        for _ in range(samples_per_class):
            subject = random.choice(content["subjects"])
            condition = random.choice(content["conditions"])
            location = random.choice(content["locations"])
            action = random.choice(action_phrases)
            connector = random.choice(connector_phrases)
            
            templates = [
                f"{subject} kondisi {condition} {location} {action}.",
                f"{connector} {subject} {condition} {location}, {action}",
                f"Laporan: {subject} ditemukan {condition} di {location}. {action}",
                f"{subject} mengalami {condition} saat {location}. Harus {action}",
                f"{subject} {condition} {location} - {action}"
            ]
            
            text = random.choice(templates)
            
            rows.append({
                "text": text,
                "category": category
            })
    
    df = pd.DataFrame(rows)
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    
    return df

if __name__ == "__main__":
    print("Generating skin tone dataset...")
    skin_df = generate_dataset_samples(500)
    skin_df.to_csv(os.path.join("data", "skin_detection_dataset.csv"), index=False)
    print(f"Saved skin detection dataset: {len(skin_df)} samples")
    
    print("\nGenerating waste quality dataset...")
    waste_df = generate_waste_quality_dataset(300)
    waste_df.to_csv(os.path.join("data", "waste_quality_dataset_expanded.csv"), index=False)
    print(f"Saved waste quality dataset: {len(waste_df)} samples")
    
    print("\nDataset Summary:")
    print(skin_df["category"].value_counts())
    print("\nWaste Category Distribution:")
    print(waste_df["category"].value_counts())
