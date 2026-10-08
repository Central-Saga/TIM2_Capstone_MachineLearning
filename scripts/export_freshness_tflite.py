import os
import sys

def build_and_export():
    import tensorflow as tf
    print(f"TensorFlow version: {tf.__version__}")

    # Build a lightweight model matching PRD Section 9
    # Input: 224x224x3, Float32 in [-1.0, 1.0]
    # Output: 4 classes (FRESH, ACCEPTABLE, SPOILED, REJECT)
    inputs = tf.keras.Input(shape=(224, 224, 3), name="input_image")
    
    # Feature extractor (lightweight CNN)
    x = tf.keras.layers.Conv2D(16, (3, 3), strides=2, padding="same", activation="relu")(inputs)
    x = tf.keras.layers.DepthwiseConv2D((3, 3), padding="same", activation="relu")(x)
    x = tf.keras.layers.Conv2D(32, (1, 1), activation="relu")(x)
    x = tf.keras.layers.MaxPool2D((2, 2))(x)
    
    x = tf.keras.layers.DepthwiseConv2D((3, 3), padding="same", activation="relu")(x)
    x = tf.keras.layers.Conv2D(64, (1, 1), activation="relu")(x)
    x = tf.keras.layers.MaxPool2D((2, 2))(x)
    
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(32, activation="relu")(x)
    outputs = tf.keras.layers.Dense(4, activation="softmax", name="freshness_probabilities")(x)
    
    model = tf.keras.Model(inputs=inputs, outputs=outputs, name="KitchenGuard_Freshness_v1")
    model.summary()

    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    # Save to android assets
    output_dir = os.path.join("android", "app", "src", "main", "assets")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "freshness-v1.tflite")

    with open(output_path, "wb") as f:
        f.write(tflite_model)

    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"SUCCESS: Exported {output_path} ({size_mb:.2f} MB)")

if __name__ == "__main__":
    build_and_export()
