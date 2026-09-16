"""
KitchenGuard CSM - Enhanced Image Dataset Generator
Untuk training object detection dan classification dari kamera
Generates synthetic images dengan various ingredients
"""

import os
import random
import numpy as np
import pandas as pd
from PIL import Image, ImageDraw, ImageFont
import cv2

random.seed(42)
np.random.seed(42)

# Ingredient categories dengan color ranges
INGREDIENT_DATA = {
    'vegetables': {
        'tomato': {
            'color': (220, 50, 50),
            'shape': 'sphere',
            'size': (30, 50),
            'label': 'Tomat'
        },
        'potato': {
            'color': (180, 160, 120),
            'shape': 'oval',
            'size': (40, 60),
            'label': 'Kentang'
        },
        'carrot': {
            'color': (255, 140, 60),
            'shape': 'cylinder',
            'size': (20, 70),
            'label': 'Wortel'
        },
        'cucumber': {
            'color': (34, 139, 34),
            'shape': 'cylinder',
            'size': (25, 80),
            'label': 'Mentimun'
        },
        'eggplant': {
            'color': (90, 50, 120),
            'shape': 'oval',
            'size': (35, 60),
            'label': 'Terong'
        },
        'pepper_red': {
            'color': (220, 60, 60),
            'shape': 'irregular',
            'size': (40, 50),
            'label': 'Paprika Merah'
        },
        'pepper_green': {
            'color': (100, 180, 80),
            'shape': 'irregular',
            'size': (40, 50),
            'label': 'Paprika Hijau'
        },
        'onion': {
            'color': (200, 180, 150),
            'shape': 'sphere',
            'size': (45, 55),
            'label': 'Bawang Bombay'
        },
        'garlic': {
            'color': (240, 240, 220),
            'shape': 'cluster',
            'size': (30, 40),
            'label': 'Bawang Putih'
        },
        'broccoli': {
            'color': (35, 120, 40),
            'shape': 'bushy',
            'size': (50, 70),
            'label': 'Brokoli'
        }
    },
    'fruits': {
        'apple_red': {
            'color': (200, 30, 40),
            'shape': 'sphere',
            'size': (35, 45),
            'label': 'Apel Merah'
        },
        'apple_green': {
            'color': (100, 180, 60),
            'shape': 'sphere',
            'size': (35, 45),
            'label': 'Apel Fuji'
        },
        'banana': {
            'color': (255, 220, 50),
            'shape': 'crescent',
            'size': (30, 80),
            'label': 'Pisang'
        },
        'orange': {
            'color': (255, 140, 50),
            'shape': 'sphere',
            'size': (40, 50),
            'label': 'Jeruk'
        },
        'grape': {
            'color': (80, 50, 120),
            'shape': 'cluster',
            'size': (40, 50),
            'label': 'Anggur'
        },
        'strawberry': {
            'color': (220, 40, 60),
            'shape': 'heart',
            'size': (25, 35),
            'label': 'Stroberi'
        }
    },
    'meat': {
        'beef': {
            'color': (180, 40, 40),
            'shape': 'flat_slice',
            'size': (80, 100),
            'label': 'Daging Sapi'
        },
        'chicken': {
            'color': (240, 200, 180),
            'shape': 'breast',
            'size': (70, 90),
            'label': 'Daging Ayam'
        },
        'fish_salmon': {
            'color': (255, 160, 120),
            'shape': 'fillet',
            'size': (90, 120),
            'label': 'Salmon'
        },
        'fish_tuna': {
            'color': (180, 50, 60),
            'shape': 'fillet',
            'size': (90, 120),
            'label': 'Tuna'
        }
    },
    'dairy': {
        'cheese': {
            'color': (255, 220, 80),
            'shape': 'wedge',
            'size': (60, 80),
            'label': 'Keju'
        },
        'milk_pour': {
            'color': (255, 255, 240),
            'shape': 'liquid',
            'size': (100, 100),
            'label': 'Susu'
        }
    },
    'grains': {
        'rice': {
            'color': (255, 255, 240),
            'shape': 'pile',
            'size': (60, 60),
            'label': 'Beras'
        },
        'noodles': {
            'color': (255, 240, 180),
            'shape': 'tangled',
            'size': (80, 80),
            'label': 'Mie'
        }
    }
}

def create_background(width=200, height=200):
    """Create kitchen background"""
    bg_color = random.choice([
        (245, 245, 220),   # Kitchen counter beige
        (220, 230, 240),   # Light blue
        (200, 210, 200),   # Grey marble
        (230, 220, 210)    # Wooden texture
    ])
    
    img = Image.new('RGB', (width, height), bg_color)
    draw = ImageDraw.Draw(img)
    
    # Add some texture/variation
    for _ in range(random.randint(20, 50)):
        x1 = random.randint(0, width)
        y1 = random.randint(0, height)
        x2 = x1 + random.randint(5, 20)
        y2 = y1 + random.randint(5, 20)
        draw.rectangle([x1, y1, x2, y2], fill=(random.randint(230, 255), 
                                                   random.randint(230, 255), 
                                                   random.randint(220, 255)))
    
    return img

def draw_ingredient(img_draw, item_config, position, scale=1.0):
    """Draw ingredient based on configuration"""
    color = item_config['color']
    shape = item_config['shape']
    size = tuple(int(s * scale) for s in item_config['size'])
    
    base_x, base_y = position
    
    if shape == 'sphere':
        # Draw circle/sphere
        img_draw.ellipse([base_x - size[0]//2, base_y - size[1]//2,
                         base_x + size[0]//2, base_y + size[1]//2],
                        fill=color, outline=(max(0, color[0]-40), max(0, color[1]-40), max(0, color[2]-40)))
        
    elif shape == 'oval':
        # Draw oval
        img_draw.ellipse([base_x - size[0], base_y - size[1]//2,
                         base_x + size[0], base_y + size[1]//2],
                        fill=color, outline=(max(0, color[0]-30), max(0, color[1]-30), max(0, color[2]-30)))
    
    elif shape == 'cylinder':
        # Draw cylinder/cilinder
        points = [(base_x - size[0], base_y), (base_x, base_y - size[1]//2),
                 (base_x + size[0], base_y)]
        img_draw.polygon(points, fill=color, outline=(max(0, color[0]-30), max(0, color[1]-30), max(0, color[2]-30)))
    
    elif shape == 'irregular':
        # Draw irregular shape (like pepper)
        points = []
        num_points = 6
        for i in range(num_points):
            angle = 2 * np.pi * i / num_points
            r = size[0] * random.uniform(0.7, 1.3)
            x = base_x + r * np.cos(angle)
            y = base_y + r * np.sin(angle)
            points.append((int(x), int(y)))
        img_draw.polygon(points, fill=color, outline=(max(0, color[0]-30), max(0, color[1]-30), max(0, color[2]-30)))
    
    elif shape == 'cluster':
        # Draw multiple small circles (like grapes, garlic)
        cluster_size = size[0] // 3
        for _ in range(random.randint(5, 10)):
            cx = base_x + random.randint(-cluster_size, cluster_size)
            cy = base_y + random.randint(-cluster_size//2, cluster_size//2)
            img_draw.ellipse([cx - cluster_size//2, cy - cluster_size//2,
                             cx + cluster_size//2, cy + cluster_size//2],
                            fill=color, outline=(max(0, color[0]-20), max(0, color[1]-20), max(0, color[2]-20)))
    
    # Add shadow
    shadow_color = tuple(max(0, c - 80) for c in color)
    shadow_offset = random.randint(5, 15)
    if shape in ['sphere', 'oval']:
        img_draw.ellipse([base_x - size[0]//2 + shadow_offset, base_y + size[1]//2,
                         base_x + size[0]//2 + shadow_offset, base_y + size[1]//2 + shadow_offset//2],
                        fill=shadow_color)

def generate_single_ingredient_image(ingredient_type, category):
    """Generate single ingredient image with variations"""
    
    img_width, img_height = 224, 224  # Standard ML input size
    
    # Create background
    img = create_background(img_width, img_height)
    img_draw = ImageDraw.Draw(img)
    
    # Get ingredient config
    ing_data = INGREDIENT_DATA[category][ingredient_type]
    
    # Random scale and position
    scale = random.uniform(0.7, 1.3)
    pos_x = random.randint(img_width//4, img_width*3//4)
    pos_y = random.randint(img_height//3, img_height*2//3)
    
    # Draw ingredient
    draw_ingredient(img_draw, ing_data, (pos_x, pos_y), scale)
    
    # Add noise and lighting variations
    np_img = np.array(img)
    noise = np.random.normal(0, 10, np_img.shape).astype(np.int16)
    brightness = random.randint(240, 280)
    np_img = np.clip(np_img.astype(np.int16) + noise, 0, 255).astype(np.uint8)
    np_img = (np_img * brightness / 255).astype(np.uint8)
    
    img = Image.fromarray(np_img)
    
    # Add metadata
    metadata = {
        'image_id': f"IMG_{category.upper()}_{ingredient_type}_{random.randint(1000, 9999)}",
        'category': category,
        'ingredient': ingredient_type,
        'display_name': ing_data['label'],
        'rgb_mean': float(np.mean(np_img[:,:,0])),  # R
        'rgb_mean_g': float(np.mean(np_img[:,:,1])),  # G
        'rgb_mean_b': float(np.mean(np_img[:,:,2])),  # B
        'freshness': random.choice(['FRESH', 'GOOD', 'FAIR']),
        'quality_score': random.uniform(0.7, 1.0),
        'lighting': random.choice(['NORMAL', 'BRIGHT', 'DIM', 'SHADOW']),
        'background': random.choice(['COUNTER', 'BOARD', 'PLATE', 'HAND'])
    }
    
    return img, metadata

def generate_dataset(total_images_per_category=200):
    """Generate complete dataset with all ingredients"""
    
    all_metadata = []
    
    categories = list(INGREDIENT_DATA.keys())
    
    for category in categories:
        ingredients = list(INGREDIENT_DATA[category].keys())
        
        for ingredient in ingredients:
            for i in range(total_images_per_category):
                try:
                    img, metadata = generate_single_ingredient_image(ingredient, category)
                    metadata['image_index'] = len(all_metadata)
                    all_metadata.append(metadata)
                    
                    if (i + 1) % 50 == 0:
                        print(f"Generated {(i+1)/total_images_per_category*100:.0f}% of {category}/{ingredient}")
                        
                except Exception as e:
                    print(f"Error generating {category}/{ingredient} #{i}: {e}")
                    continue
    
    df = pd.DataFrame(all_metadata)
    
    # Shuffle dataset
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    
    return df

if __name__ == "__main__":
    print("="*60)
    print("KitchenGuard CSM - Enhanced Image Dataset Generator")
    print("="*60)
    
    total_images = 200  # Images per ingredient
    
    print(f"\nGenerating {total_images} images per ingredient...")
    print("Categories:")
    for cat in INGREDIENT_DATA:
        print(f"  - {cat}: {len(INGREDIENT_DATA[cat])} ingredients")
    
    # Generate dataset
    print("\n[1/2] Generating images...")
    df = generate_dataset(total_images)
    
    # Save to CSV
    print("\n[2/2] Saving metadata...")
    output_path = "data/image_ingest_metadata.csv"
    df.to_csv(output_path, index=False)
    
    print(f"\n✓ Saved: {output_path}")
    print(f"  Total images: {len(df)}")
    print(f"  Categories: {df['category'].nunique()}")
    print(f"  Ingredients: {df['ingredient'].nunique()}")
    
    print("\nDataset Statistics:")
    print("-" * 40)
    print(f"By Category:\n{df['category'].value_counts()}")
    print(f"\nBy Ingredient:\n{df.groupby('category')['ingredient'].count()}")
    
    print("\nSample Metadata:")
    print(df.head())
    
    print("\n" + "="*60)
    print("Generation Complete!")
    print("="*60)
    print("\nNext steps:")
    print("1. Use these images to train vision models")
    print("2. Convert to TFRecord or Pascal VOC format for object detection")
    print("3. Train YOLO or SSD model for real-time detection")
    print("4. Integrate with Android camera preview")
