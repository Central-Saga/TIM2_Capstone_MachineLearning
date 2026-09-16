"""
KitchenGuard CSM - Enhanced Dataset Generator v2
Membuat dataset dengan lebih banyak samples untuk akurasi lebih tinggi
"""

import os
import random
import numpy as np
import pandas as pd

random.seed(42)
np.random.seed(42)

# Fair Skin Ranges (Enhanced dengan variasi lebih luas)
FAIR_SKIN_RANGES = {
    "FAIR_1": {"min_rgb": [235, 215, 195], "max_rgb": [255, 240, 220], "description": "Sangat terang, mudah terbakar"},
    "FAIR_2": {"min_rgb": [225, 205, 185], "max_rgb": [245, 230, 215], "description": "Terang, cenderung terbakar"},
    "FAIR_3": {"min_rgb": [215, 195, 175], "max_rgb": [235, 220, 205], "description": "Terang dengan sedikit tan"},
}

def generate_enhanced_skin_dataset():
    """Generate skin detection dataset dengan samples lebih banyak dan variatif"""
    rows = []
    image_counter = 1
    
    # Generate lebih banyak samples per kategori
    for category in ["HAND", "FACE"]:
        for skin_type in FAIR_SKIN_RANGES.keys():
            # 600 samples per skin type per category = 1200 x 2 = 2400 total
            for i in range(600):
                range_data = FAIR_SKIN_RANGES[skin_type]
                
                # RGB values dengan variasi natural
                r = random.randint(range_data["min_rgb"][0], range_data["max_rgb"][0])
                g = random.randint(range_data["min_rgb"][1], range_data["max_rgb"][1])
                b = random.randint(range_data["min_rgb"][2], range_data["max_rgb"][2])
                
                # Tambah noise natural
                noise_r = random.randint(-12, 12)
                noise_g = random.randint(-10, 10)
                noise_b = random.randint(-8, 8)
                
                r = max(180, min(255, r + noise_r))
                g = max(160, min(255, g + noise_g))
                b = max(140, min(255, b + noise_b))
                
                # Lighting conditions dengan distribusi balance
                lighting_options = ["NORMAL", "BRIGHT", "DIM"]
                weights = [0.5, 0.3, 0.2]  # NORMAL lebih umum
                
                rows.append({
                    "image_id": f"SKIN_{category}_{image_counter:05d}",
                    "category": category,
                    "skin_type": skin_type,
                    "rgb_mean_r": float(r),
                    "rgb_mean_g": float(g),
                    "rgb_mean_b": float(b),
                    "rgb_std_r": float(abs(random.gauss(6, 3))),
                    "rgb_std_g": float(abs(random.gauss(5, 3))),
                    "rgb_std_b": float(abs(random.gauss(4, 3))),
                    "lighting_condition": random.choices(lighting_options, weights=weights)[0],
                    "description": FAIR_SKIN_RANGES[skin_type]["description"],
                    "hygiene_note": random.choice([
                        "Clean visible skin detected",
                        "Hand washing required",
                        "Gloves must be worn",
                        "Proper hygenic practice observed",
                        "Skin contact noted",
                        "Hygiene check pending"
                    ])
                })
                image_counter += 1
    
    df = pd.DataFrame(rows)
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    
    return df


def generate_enhanced_waste_dataset(samples_per_class=500):
    """Generate waste classification dataset dengan lebih banyak samples & variasi"""
    rows = []
    
    # Kategori yang lebih lengkap dengan subjects lebih banyak
    categories = {
        "CONTAMINATED": {
            "subjects": [
                "Salad terkontaminasi hair", "Pasta mengandung debu", 
                "Rice mixture menyentuh lantai", "Soup terkena insect",
                "Sandwich terpapar fly", "Vegetable terkontaminasi dirt",
                "Meat product bersentuhan raw food", "Dessert dengan foreign material"
            ],
            "conditions": ["terkontaminasi", "tercemar", "tersentuh benda asing", "terjangkit kuman"],
            "locations": ["di preparation area", "di serving line", "di storage bin", "after dropping"],
        },
        "SPOILED": {
            "subjects": [
                "Ayam berbau busuk", "Milk berjamur", "Egg berlendir",
                "Fish bau ammonia", "Cream terpisah curdle", "Butter tengik",
                "Fruit membusuk overripe", "Vegetable layu brown"
            ],
            "conditions": ["berjamur", "busuk", "berlendir", "berbau tidak sedap", "muncul discoloration"],
            "locations": ["in cooler storage", "expired display", "long-term stock", "back storage room"],
        },
        "EXPIRED": {
            "subjects": [
                "Mayonaise expired date", "Ketchup kedaluwarsa", "Sauce MHD terlewati",
                "Spices kadaluarsa", "Oil rusak kimia", "Flour serangga",
                "Sugar harden clump", "Yeast tidak aktif"
            ],
            "conditions": ["expired date", "kedaluwarsa", "MHD terlewati", "beyond shelf life", "past expiry"],
            "locations": ["in dry storage", "pantry inventory", "backup supply", "rotation failure"],
        },
        "OVERCOOKED": {
            "subjects": [
                "Steak terlalu gosong", "Chicken burn exterior", "Fish overdone dry",
                "Vegetables charcoal burnt", "Potato blackened crisp", "Rice stuck pan",
                "Bread crust too dark", "Egg rubbery tough"
            ],
            "conditions": ["gosong", "hangus", "overdone", "burnt exterior", "charred surface"],
            "locations": ["after cooking service", "during quality check", "plate inspection", "kitchen timer error"],
        },
        "PREP_WASTE": {
            "subjects": [
                "Kulit kentang prep station", "Tomato stem trimming", "Carel peeling waste",
                "Onion outer layers", "Herb stalks discarded", "Fish bones skeleton",
                "Shrimp shell exoskeleton", "Fruit cores seeds"
            ],
            "conditions": ["trimming waste", "peeling discard", "preparation byproduct", "natural waste part"],
            "locations": ["prep station bin", "compost container", "vegetable processing area", "knife board scraps"],
        },
        "SURPLUS": {
            "subjects": [
                "Portion rice tidak terjual", "Grilled chicken tidak tersentuh",
                "Steamed broccoli leftover", "Fresh fruit salad unserved",
                "Chicken stock excess", "Baked goods day-old", "Sauce remainder batch"
            ],
            "conditions": ["tidak terjual hari ini", "over-prepared", "leftovers after service", "unserved portion", "excess production"],
            "locations": ["holding warmer", "service line end", "after dinner rush", "buffet closing"],
        }
    }
    
    action_phrases = [
        "wajib segera dibuang sesuai SOP",
        "harus dibuatkan waste log entry",
        "direject oleh kitchen manager",
        "noted dalam daily waste report",
        "disposal required immediately",
        "mandatory waste documentation",
        "critical hygiene violation logged"
    ]
    
    connector_phrases = [
        "", "catatan chef:", "laporan staff shift:", "inspeksi menemukan", 
        "kondisi aktual:", "observation record:", "kitchen audit found:"
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
                f"{subject} {condition} {location} - {action}",
                f"Found: {subject} {condition} at {location}. {action}",
                f"Inspection result: {subject} is {condition}. Required {action}"
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
    print("="*70)
    print("🍽️ KitchenGuard CSM - Enhanced Dataset Generator v2")
    print("="*70)
    
    # Generate Enhanced Skin Detection Dataset
    print("\n📊 [1/2] Generating ENHANCED skin tone dataset...")
    skin_df = generate_enhanced_skin_dataset()
    os.makedirs("../data", exist_ok=True)
    skin_df.to_csv(os.path.join("..", "data", "skin_detection_dataset.csv"), index=False)
    print(f"✓ Saved skin detection dataset: {len(skin_df)} samples (ENHANCED!)")
    print(f"  - Categories:")
    print(f"    • HAND: {len(skin_df[skin_df['category']=='HAND'])}")
    print(f"    • FACE: {len(skin_df[skin_df['category']=='FACE'])}")
    print(f"  - Skin Types Distribution:")
    print(f"    • FAIR_1: {len(skin_df[skin_df['skin_type']=='FAIR_1'])}")
    print(f"    • FAIR_2: {len(skin_df[skin_df['skin_type']=='FAIR_2'])}")
    print(f"    • FAIR_3: {len(skin_df[skin_df['skin_type']=='FAIR_3'])}")
    
    # Generate Enhanced Waste Classification Dataset
    print("\n♻️  [2/2] Generating ENHANCED waste quality dataset...")
    waste_df = generate_enhanced_waste_dataset(500)  # Increased from 300 to 500
    waste_df.to_csv(os.path.join("..", "data", "waste_quality_dataset_expanded.csv"), index=False)
    print(f"✓ Saved waste quality dataset: {len(waste_df)} samples (ENHANCED!)")
    print(f"  - Category distribution:")
    for cat, count in waste_df['category'].value_counts().items():
        print(f"    • {cat}: {count}")
    
    print("\n" + "="*70)
    print("✅ Dataset Generation Complete!")
    print("="*70)
    print(f"\n📁 Files saved to: ../data/")
    print("  1. 🧴 skin_detection_dataset.csv - {0} samples".format(len(skin_df)))
    print("  2. ♻️  waste_quality_dataset_expanded.csv - {0} samples".format(len(waste_df)))
    print("\n🚀 Next steps:")
    print("   python train_skin_model.py")
    print("   python train_improved_waste_model.py")
    print("\n💡 Expected Accuracy Improvements:")
    print("   • Skin Detection: 95%+ (from enhanced samples)")
    print("   • Waste Classifier: 98%+ (from diverse text variations)")
    print("="*70)
