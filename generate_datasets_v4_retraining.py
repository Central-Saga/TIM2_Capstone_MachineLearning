#!/usr/bin/env python3
"""
KitchenGuard ML - Enhanced Dataset Generation v4 (RETRAINING VERSION)
Generates enhanced dataset dengan:
1. Expanded known waste patterns per category
2. UNIDENTIFIABLE samples untuk rejection learning
3. Better keyword coverage
"""

import os
import random
import numpy as np
import pandas as pd

# Setup paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
os.makedirs(DATA_DIR, exist_ok=True)

random.seed(42)
np.random.seed(42)

print("="*80)
print("ENHANCED DATASET GENERATOR V4 - RETRAINING EDITION")
print("="*80)
print("Data directory: {0}".format(DATA_DIR))

# ============================================
# PART 1: EXPANDED KNOWN WASTE PATTERNS
# ============================================
print("\n[1/2] Generating ENHANCED waste classification dataset...")

categories_expanded = {
    "CONTAMINATED": {
        "subjects": [
            "Salad terkontaminasi hair", "Pasta mengandung debu", 
            "Rice mixture menyentuh lantai", "Soup terkena insect",
            "Sandwich terpapar fly", "Vegetable terkontaminasi dirt",
            "Meat product bersentuhan raw food", "Dessert dengan foreign material",
            "Ikan fillet jatuh ke lantai kotor", "Ayam mentah tersentuh air kotor"
        ],
        "conditions": [
            "terkontaminasi", "tercemar", "tersentuh benda asing", "terjangkit kuman",
            "kemasukan serangga lalat", "jatuh ke lantai", "kena debu kotor",
            "tercemarkan sabun cuci", "kontak dengan bahan berbahaya"
        ],
        "locations": [
            "di preparation area", "di serving line", "di storage bin", 
            "after dropping", "di lantai kitchen", "dekat sink cuci piring",
            "area prep station yang kotor"
        ],
        "keywords": [
            "terkontaminasi", "lantai kotor", "hair", "insect", "fly", 
            "debu kotor", "sabun", "benda asing", "berminyak", "air kotor",
            "tercemarkan", "kontak langsung"
        ]
    },
    "SPOILED": {
        "subjects": [
            "Ayam berbau busuk", "Milk berjamur", "Egg berlendir",
            "Fish bau ammonia", "Cream terpisah curdle", "Butter tengik",
            "Fruit membusuk overripe", "Vegetable layu brown",
            "Daging sapi berubah kehitaman", "Susu asam tidak layak konsumsi"
        ],
        "conditions": [
            "berjamur", "busuk", "berlendir", "berbau tidak sedap", 
            "muncul discoloration", "telah berjamur putih", "berubah warna hitam",
            "mengeluarkan lendir", "bau asam menyengat", "teksturnya lembek"
        ],
        "locations": [
            "in cooler storage", "expired display", "long-term stock", 
            "back storage room", "dalam chiller", "cold storage dapur"
        ],
        "keywords": [
            "berbau busuk", "berlendir", "berjamur", "busuk", "membusuk",
            "berubah kehitaman", "berbau asam", "telah berjamur", "menggelembung",
            "bau ammonia", "tengik", "curdle", "overripe"
        ]
    },
    "EXPIRED": {
        "subjects": [
            "Mayonaise expired date", "Ketchup kedaluwarsa", "Sauce MHD terlewati",
            "Spices kadaluarsa", "Oil rusak kimia", "Flour serangga",
            "Sugar harden clump", "Yeast tidak aktif", "Botol kecap lewat date",
            "Susu UHT sudah expired"
        ],
        "conditions": [
            "expired date", "kedaluwarsa", "MHD terlewati", "beyond shelf life", 
            "past expiry", "lewat batas tanggal", "sudah melewati masa simpan",
            "tidak lagi layak pakai", "batas konsumsi aman terlampaui"
        ],
        "locations": [
            "in dry storage", "pantry inventory", "backup supply", 
            "rotation failure", "rak penyimpanan kering", "gudang bahan baku"
        ],
        "keywords": [
            "expired", "expired date", "kedaluwarsa", "lewat date", 
            "MHD terlewati", "telah melewati", "batas tanggal konsumsi",
            "masa simpan habis", "kadaluarsa", "expired date tercantum"
        ]
    },
    "OVERCOOKED": {
        "subjects": [
            "Steak terlalu gosong", "Chicken burn exterior", "Fish overdone dry",
            "Vegetables charcoal burnt", "Potato blackened crisp", "Rice stuck pan",
            "Bread crust too dark", "Egg rubbery tough", "Pizza hangus oven",
            "Nasi gosong dasar panci"
        ],
        "conditions": [
            "gosong", "hangus", "overdone", "burnt exterior", "charred surface",
            "terbakar di oven", "kelewat matang", "kehilangan tekstur",
            "kulit keras kerak", "bau sangit terbakar"
        ],
        "locations": [
            "after cooking service", "during quality check", "plate inspection", 
            "kitchen timer error", "setelah dipanggang", "kompor ditinggal"
        ],
        "keywords": [
            "gosong", "hangus", "terbakar", "overdone", "burnt", "charred",
            "terlalu lama", "kelewat matang", "kurang air", "kerak",
            "hitam karena", "bau sangit", "overcooked"
        ]
    },
    "PREP_WASTE": {
        "subjects": [
            "Kulit kentang prep station", "Tomato stem trimming", "Carel peeling waste",
            "Onion outer layers", "Herb stalks discarded", "Fish bones skeleton",
            "Shrimp shell exoskeleton", "Fruit cores seeds",
            "Kulit wortel bonggol brokoli", "Biji melon sisa fruit salad"
        ],
        "conditions": [
            "trimming waste", "peeling discard", "preparation byproduct", 
            "natural waste part", "sisa kupasan", "bonggol sayuran",
            "bagian tidak digunakan", "limbah persiapan dapur"
        ],
        "locations": [
            "prep station bin", "compost container", "vegetable processing area", 
            "knife board scraps", "meja chopping station", "talenan preparation"
        ],
        "keywords": [
            "kulit", "bonggol", "sisa kupasan", "stemming", "peeling", 
            "preparation waste", "sisa pemecahan telur", "biji melon",
            "tomato stem", "fruit cores", "fish bones", "shrimp shell",
            "onion outer layers", "herb stalks"
        ]
    },
    "SURPLUS": {
        "subjects": [
            "Portion rice tidak terjual", "Grilled chicken tidak tersentuh",
            "Steamed broccoli leftover", "Fresh fruit salad unserved",
            "Chicken stock excess", "Baked goods day-old", "Sauce remainder batch",
            "Nasi tumpeng sisa gathering", "Sup buffet tidak habis dikonsumsi"
        ],
        "conditions": [
            "tidak terjual hari ini", "over-prepared", "leftovers after service", 
            "unserved portion", "excess production", "lebih dari kebutuhan",
            "sisa prasmanan", "kelebihan porsi masak", "tidak terkonsumsi tamu"
        ],
        "locations": [
            "holding warmer", "service line end", "after dinner rush", 
            "buffet closing", "display showcase", "room service holding"
        ],
        "keywords": [
            "kelebihan", "sisa buffet", "tidak terjual", "lebih awal",
            "overshoot", "leftover", "tidak habis dikonsumsi", "excess portion",
            "sisa catering", "meal not served", "extra production",
            "gathering leftover", "pramasteran tidak habis"
        ]
    }
}

# Generate expanded samples with keywords
def generate_waste_samples(category_info, samples_per_class=500):
    rows = []
    
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
    
    templates = [
        "{subject} kondisi {condition} {location} {action}.",
        "{connector} {subject} {condition} {location}, {action}",
        "Laporan: {subject} ditemukan {condition} di {location}. {action}",
        "{subject} mengalami {condition} saat {location}. Harus {action}",
        "{subject} {condition} {location} - {action}",
    ]
    
    for _ in range(samples_per_class):
        subject = random.choice(category_info["subjects"])
        condition = random.choice(category_info["conditions"])
        location = random.choice(category_info["locations"])
        action = random.choice(action_phrases)
        connector = random.choice(connector_phrases)
        
        # Use templates
        text = random.choice(templates).format(
            subject=subject, condition=condition, location=location,
            action=action, connector=connector
        )
        
        rows.append({
            "text": text,
            "category": category_info["name"] if "name" in category_info else list(categories_expanded.keys())[0]
        })
    
    return rows

# Collect all samples
expanded_rows = []

for cat_name, cat_info in categories_expanded.items():
    print("  Processing category: {0} ({1} keywords)...".format(
        cat_name, len(cat_info.get('keywords', []))))
    
    # Add samples per category
    cat_copy = cat_info.copy()
    cat_copy["name"] = cat_name
    samples = generate_waste_samples(cat_copy, samples_per_class=500)
    expanded_rows.extend(samples)

# Shuffle and save
df = pd.DataFrame(expanded_rows)
df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)

# Save expanded dataset
expanded_file = os.path.join(DATA_DIR, "waste_quality_dataset_enhanced_v4.csv")
df.to_csv(expanded_file, index=False)

print("✓ Saved: {0}".format(expanded_file))
print("  Samples: {0} (EXPANDED!)".format(len(df)))
print("  Category distribution:")
for cat, count in df['category'].value_counts().items():
    print("    • {0}: {1}".format(cat, count))

# ============================================
# PART 2: UNKNOWN/NEGATIVE SAMPLES FOR REJECTION
# ============================================
print("\n[2/2] Generating UNKNOWN/UNIDENTIFIABLE samples for rejection learning...")

unknown_categories = {
    "UNIDENTIFIABLE": {
        "samples": [
            # Too vague/general
            "barang rusak",
            "material tidak dikenali",
            "benda aneh",
            "sesuatu yang rusak",
            "ada yang salah",
            "produk bermasalah",
            "item quality issue",
            
            # Non-waste related
            "xyz abc def ghi jkl mno",
            "qwerty random text",
            "abcdefg sample string",
            "test input data",
            "dummy unknown phrase",
            
            # Incomplete/broken phrases
            "sisa makanan",
            "barang tidak jelas",
            "material rusak ringan",
            "something broken here",
            "unknown waste type",
            
            # Ambiguous descriptions
            "warna biru aneh",
            "bentuk tidak biasa",
            "bau tidak familiar",
            "tekstur mencurigakan",
            "kelihatan tidak bagus",
            
            # Typo variations that won't match patterns
            "brang rusak",
            "materiil tdak dikenali",
            "zml materi",
            "barang rasuk",
            
            # Mixed languages/confused inputs
            "this is wrong item",
            "incorrect material code",
            "wrong barcode detected",
            "error in system",
            
            # Edge cases that need human review
            "mungkin busuk tapi不确定",
            "belum tentu expired",
            "perlu dicek lebih lanjut",
            "cek ulang dulu",
            "apakah boleh dibuang?",
        ],
        "count": 200
    }
}

unknown_rows = []
total_unknown = 0

for cat_name, cat_info in unknown_categories.items():
    print("  Generating UNKNOWN samples ({0} count)...".format(cat_info["count"]))
    
    # Repeat samples to reach target count
    base_samples = cat_info["samples"]
    repetitions = (cat_info["count"] + len(base_samples) - 1) // len(base_samples)
    
    for _ in range(repetitions):
        unknown_rows.append({
            "text": random.choice(base_samples),
            "category": cat_name
        })
    
    total_unknown = len(unknown_rows)

# Truncate to target count
unknown_df = pd.DataFrame(unknown_rows[:cat_info["count"]])

# Save unknown samples separately for training
unknown_file = os.path.join(DATA_DIR, "unknown_unidentifiable_samples.csv")
unknown_df.to_csv(unknown_file, index=False)

print("✓ Saved: {0}".format(unknown_file))
print("  Samples: {0} (NEW REJECTION LEARNING DATA!)".format(len(unknown_df)))
print("  Sample examples:")
for i, row in unknown_df.head(5).iterrows():
    print("    \"{0}\"".format(row['text'][:60]))

# ============================================
# PART 3: FINAL COMBINATIONS
# ============================================
print("\n" + "="*80)
print("DATASET GENERATION COMPLETE!")
print("="*80)

# Combine expanded + unknown
all_combined = pd.concat([df, unknown_df], ignore_index=True)
all_combined = all_combined.sample(frac=1.0, random_state=42).reset_index(drop=True)

# Save combined dataset
combined_file = os.path.join(DATA_DIR, "waste_classification_complete_v4.csv")
all_combined.to_csv(combined_file, index=False)

print("\n📊 FINAL STATISTICS:")
print("-" * 80)
print("Total Combined Samples: {0}".format(len(all_combined)))
print("\nCategory Distribution:")
for cat, count in all_combined['category'].value_counts().items():
    percentage = count / len(all_combined) * 100
    icon = "✓" if cat != "UNIDENTIFIABLE" else "⊘"
    print("  {0} {1:<20}: {2:>4} ({3:.1f}%)".format(icon, cat, count, percentage))

print("\n📁 Output Files:")
print("  1. ✓ waste_quality_dataset_enhanced_v4.csv     - {0} expanded waste samples".format(len(df)))
print("  2. ⊘ unknown_unidentifiable_samples.csv       - {0} rejection samples".format(len(unknown_df)))
print("  3. ✓ waste_classification_complete_v4.csv     - Complete dataset ({0})".format(len(all_combined)))

print("\n🚀 NEXT STEPS FOR RETRAINING:")
print("-" * 80)
print("1. Run preprocessing on complete dataset")
print("   python src/preprocess.py")
print("")
print("2. Train new model with both CONFIRMED and UNKNOWN classes")
print("   python scripts/train_improved_waste_model.py --include-unknown")
print("")
print("3. Validate performance on test set")
print("   python validate_model_advanced.py")
print("")
print("4. Deploy updated model with 2-skema logic")

print("\n" + "="*80)
