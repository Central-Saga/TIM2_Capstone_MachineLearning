"""
KitchenGuard CSM - OOV (Out of Vocabulary) Test Dataset Generator
===================================================================
Generate challenging test cases that are OUTSIDE the training vocabulary.
This simulates real-world scenarios where users enter texts not seen during training.

Purpose:
- Test system robustness on unknown/novel inputs
- Identify edge cases and fallback mechanisms
- Prevent overfitting to training data patterns
- Provide realistic accuracy benchmarks
"""

import random
from pathlib import Path
import csv


# =====================
# OOV TEST SCENARIOS
# =====================

# Category 1: Rare/Uncommon Waste Descriptions (Not in Training Data)
OOV_RARE_WASTE = [
    # Mixed language / Code-switching
    "Ayam goreng basi banget sih",
    "Daging sapi udah bau amonia bro",
    "Susu UHT ketumpah di lantai kitchen",
    "Roti moldy karena kelembaban tinggi",
    "Telur ceplok overcook sampai hangus",
    
    # Technical/Slang terms
    "Fish fillet terkontaminasi heavy metals warning",
    "Vegetable salad dengan insect larvae detected",
    "Rice porridge expired date sudah lewat MHD+",
    "Cheese block with penicillium fungus growth",
    "Seafood shrimp sudah discoloration hijau kebiruan",
    
    # Compound problems
    "Nasi goreng telat disajikan + kontainer bocor + tercemar debu",
    "Sup ayam dingin terlalu lama + smell off + visual contamination",
    "Salad buah dipotong hari Senin + sekarang Jumat + bruang asam",
    "Ice cream melted refreeze again + crystallized texture + taste off",
    "Bread rolls stuck dalam freezer > 3 bulan + freezer burn parah",
    
    # Unusual scenarios
    "Minyak goreng bekas digunain 5x lagi + color dark + smoke point turun",
    "Tahu tempe kadaluarsa dijadikan snack tradisional",
    "Kulit ayam burnt charcoal black karena gas flame inconsistency",
    "Tomat ceri shriveled completely dehydration extreme",
    "Gulai kambing excess coconut milk spoiled curdled texture",
]


# Category 2: Misspelled/Typo-heavy Texts (Real User Input Errors)
OOV_MISSPELLED = [
    "ayam garng rebus bju",
    "daging sofi kedaluwrs",
    "susu uht expirred tanggal",
    "roti jamur mndiri di dlm kls",
    "telur busuk baunya sgt menyengat",
    "ikan asin hnggs krna kompor",
    "sayuran layu kdaluwarsa blm dicuci",
    "mie instant lembab sdh 2 minggu",
    "biskuit soft bukan crunchy lg",
    "minyak jelantah dipakai lagi deh",
    
    # Extreme typos
    "kmdaluwrsa",
    "bdaj cpruh",
    "baui busuk skli",
    "trkontaminasi",
    "expired dtanggal thn 2024",
]


# Category 3: Complex Multi-Entity Descriptions
OOV_COMPOUND_ENTITIES = [
    "Batch WG-0915-A Daging Sapi Tenderloin 2.3kg Expired 22 Sep 2026",
    "Barcode 8991234567890 Apel Fuji Fresh Grade A Imported USA",
    "Container ABC123 Susu Ultra High Temp 1Liter Cold Chain Broken",
    "Tray XYZ789 Daging Ayam Fillet Frozen Thawed Above Safety Limit",
    "Pallet LMN456 Sayuran Organik Mix Lettuce Spinach Carrots Wilting",
    
    # Warehouse scenario
    "Zone B Chiller Unit 2 Temperature Alarm 8°C for 4 hours Meat products affected",
    "Freezer Section 3 Defrost cycle failure Ice cream blocks partially melted",
    "Dry Storage Room 5 Humidity spike Cardboard boxes water damaged grains sprouting",
    
    # Kitchen prep failure
    "Mise en place chicken breast diced raw cross-contamination contact cutting board",
    "Sauce demi-glace reduced too long viscosity too thick burned bottom pan",
    "Vegetable stock simmered overnight bacterial growth risk identified",
]


# Category 4: Non-Standard Terminology (User Creative Descriptions)
OOV_CREATIVE_DESC = [
    "Ini dagingnya kayak sudah mati lama sekali baunya",
    "Sayurnya kelihatan seperti udah nggak fresh banget deh",
    "Adonan kue nya berjamur semua lohh",
    "Minyak wangi makanan malah minyak bekas pakai lagi",
    "Bahan bakunya udah pada leleh beku dan cair terus",
    "Kuahnya rasanya aneh campur aduk weird flavor",
    "Texturenya lengket-lengket kayak belum dicuci bersih",
    "Warnanya hijau-hijau kayak ada lumut gitu",
    "Baunya kaya ammonia kimia laboratorium",
    "Kelihatan ada serangga kecil terbang-terbang di situ",
]


# Category 5: Edge Cases - Ambiguous/Unclear Inputs
OOV_AMBIGUOUS = [
    "Hmm ini kok bau ya...",
    "Kayaknya udah nggak enak deh",
    "Entahlah terlihat aneh",
    "Mungkin masih aman tapi ragu-ragu",
    "Hmm suspicious smell detected",
    "Visual inspection shows concerns",
    "Texture感觉 weird (mixed English-Chinese)",
    "Seems expired but no visible signs",
    "Could be contaminated maybe",
    "Questionable freshness status",
]


# Category 6: Domain-Specific jargon (Butcher/Marine/Food Industry Terms)
OOV_DOMAIN_JARGON = [
    "Prime rib roast marbling grade USDA Select grain-fed corn-finished",
    "Whole aquaculture salmon Atlantic farmed antibiotic-free wild-caught alternative",
    "Organic heritage breed pork loin heritage genetics heritage pork heritage label",
    "Shellfish crustacean mollusk allergen separation protocol mandatory labeling",
    "Fermented kimchi lactobacillus culture probiotic-rich gut-health functional food",
    "Aged dry-aged beef 28-day enzymatic tenderization umami enhancement technique",
    "Plant-based meat alternative mycoprotein fungal fermentation protein isolates",
    "Cold-smoked salmon salmonella listeria pathogen control HACCP verified",
    "Vacuum-sealed sous-vide pasteurization temperature-time critical control point",
    "GMO-free non-GMO project verified ingredient sourcing transparency traceability",
]


# Category 7: Negative Sentiment + Specific Complaint Patterns
OOV_COMPLAINT_PATTERNS = [
    "Staff tidak pakai sarung tangan saat handling raw chicken cross-contamination risk",
    "Chef meninggalkan daging di ambient temperature > 2 hours time-temp abuse violation",
    "Delivery vehicle refrigerator broken during transport cold chain breach suspected",
    "Supplier substituted ingredients without notice allergen cross-contact potential",
    "Kitchen sink backs-up sewage contamination food surface exposure immediate action",
    "Pest infestation rodents gnawed packaging rodent droppings visible contamination",
    "Chemical cleaning spray sprayed directly on food product chemical hazard critical",
    "Glass shard found in salad glassware breakage foreign object physical hazard",
    "Metal shavings from equipment deterioration metal fragment detection required",
    "Hair contamination customer complaint hair strands in soup biogenic hazard",
]


# =====================
# LABEL MAPPINGS
# =====================

def get_expected_category(text: str) -> str:
    """
    Map OOV input to expected waste category based on keywords/patterns
    Returns: (predicted_category, confidence_reason)
    """
    text_lower = text.lower()
    
    # CONTAMINATED indicators
    if any(kw in text_lower for kw in ["lantai", "floor", "contaminated", "terkontaminasi", 
                                        "hair", "sewage", "pest", "rodent", "shard", "metal"]):
        return "CONTAMINATED"
    
    # SPOILED indicators  
    if any(kw in text_lower for kw in ["jamur", "moldy", "mushroom", "berbau", "smell", "bau",
                                        "off", "curdled", "sour", "rancid", "discoloration"]):
        return "SPOILED"
    
    # EXPIRED indicators
    if any(kw in text_lower for kw in ["expired", "kedaluwarsa", "mhd", "date", "lewat", 
                                        "thawed", "melted", "refreeze"]):
        return "EXPIRED"
    
    # OVERCOOKED indicators
    if any(kw in text_lower for kw in ["burnt", "hangus", "gosong", "overcook", "charcoal",
                                        "black", "burned", "scorched"]):
        return "OVERCOOKED"
    
    # PREP_WASTE indicators
    if any(kw in text_lower for kw in ["trimming", "cutting", "prep", "diced", "chopped",
                                        "peeling", "stemming", "bones", "skin", "fat trim"]):
        return "PREP_WASTE"
    
    # SURPLUS indicators
    if any(kw in text_lower for kw in ["surplus", "excess", "leftover", "unserved", "not sold",
                                        "remaining", "spare", "extra batch"]):
        return "SURPLUS"
    
    # Default fallback
    return "UNKNOWN"


# =====================
# GENERATE DATASET
# =====================

def generate_oov_test_dataset(output_path: Path = None):
    """
    Generate comprehensive OOV test dataset
    Returns DataFrame-like structure suitable for evaluation
    """
    
    if output_path is None:
        output_path = Path(__file__).parent.parent / "data" / "oov_test_dataset.csv"
    
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    all_samples = []
    
    # Combine all OOV categories
    oov_categories = [
        ("RARE_WASTE", OOV_RARE_WASTE),
        ("MISSPELLED", OOV_MISSPELLED),
        ("COMPOUND_ENTITIES", OOV_COMPOUND_ENTITIES),
        ("CREATIVE_DESC", OOV_CREATIVE_DESC),
        ("AMBIGUOUS", OOV_AMBIGUOUS),
        ("DOMAIN_JARGON", OOV_DOMAIN_JARGON),
        ("COMPLAINT_PATTERNS", OOV_COMPLAINT_PATTERNS),
    ]
    
    sample_id = 1
    for category_name, samples in oov_categories:
        for sample_text in samples:
            expected_cat = get_expected_category(sample_text)
            
            all_samples.append({
                "sample_id": f"OOV_{sample_id:04d}",
                "input_text": sample_text,
                "category_type": category_name,
                "expected_category": expected_cat,
                "difficulty": "HIGH" if category_name in ["MISSPELLED", "AMBIGUOUS"] else "MEDIUM",
                "has_typo": 1 if category_name == "MISSPELLED" else 0,
                "has_mixed_language": 1 if any(kw in sample_text for kw in ["English", "Chinese", "bro", "loh"]) else 0,
                "word_count": len(sample_text.split()),
                "character_count": len(sample_text),
            })
            sample_id += 1
    
    # Write to CSV
    with open(output_path, 'w', newline='', encoding='utf-8') as csvfile:
        fieldnames = ["sample_id", "input_text", "category_type", "expected_category", 
                     "difficulty", "has_typo", "has_mixed_language", "word_count", "character_count"]
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(all_samples)
    
    print(f"✅ Generated {len(all_samples)} OOV test samples")
    print(f"   Saved to: {output_path}")
    
    # Print summary statistics
    print("\n📊 Dataset Summary:")
    print(f"   Total samples: {len(all_samples)}")
    print(f"   Categories:")
    
    from collections import Counter
    type_counts = Counter(s["category_type"] for s in all_samples)
    for cat, count in type_counts.most_common():
        print(f"      - {cat}: {count} samples")
    
    difficulty_counts = Counter(s["difficulty"] for s in all_samples)
    print(f"\n   Difficulty Levels:")
    for diff, count in difficulty_counts.items():
        print(f"      - {diff}: {count} samples")
    
    return all_samples


if __name__ == "__main__":
    print("🚀 Generating OOV (Out of Vocabulary) Test Dataset...")
    print("=" * 60)
    samples = generate_oov_test_dataset()
    print("\n✨ Generation Complete!")
    print("\n💡 Usage Instructions:")
    print("   1. Load this dataset for model evaluation")
    print("   2. Compare predictions vs expected categories")
    print("   3. Calculate F1-score on out-of-vocabulary samples")
    print("   4. Identify edge cases needing model improvement")
