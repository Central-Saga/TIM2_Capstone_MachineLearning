"""
Test script for KitchenGuard CSM ML REST API
Tests all endpoints with real-world data samples
"""

import requests
import json
from typing import Dict, Any

BASE_URL = "http://localhost:8000"


def print_section(title: str):
    """Print section header"""
    print("\n" + "=" * 70)
    print(f" {title}")
    print("=" * 70)


def print_response(endpoint: str, response: Dict[str, Any], status: str = "✅"):
    """Print formatted response"""
    print(f"\n{status} POST/GET {endpoint}")
    print(f"Status Code: {response.get('status_code', 'N/A')}")
    
    if 'json' in response and response['json']:
        print("Response:")
        print(json.dumps(response['json'], indent=2, ensure_ascii=False))
    elif 'text' in response:
        print(f"Text: {response['text'][:500]}")


def test_health_endpoint():
    """Test health check endpoint"""
    print_section("TEST 1: Health Check Endpoint")
    
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Status: {data['status']}")
            print(f"✅ Model Loaded: {data['model_loaded']}")
            print(f"✅ Preprocessing Version: {data['preprocessing_version']}")
            print(f"✅ Model Version: {data.get('version', 'N/A')}")
            
            return True
        else:
            print(f"❌ Failed with status: {response.status_code}")
            return False
            
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to server. Make sure it's running on http://localhost:8000")
        return False
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return False


def test_waste_prediction(description: str, expected_category: str = None):
    """Test waste prediction with sample text"""
    url = f"{BASE_URL}/api/ml/waste/predict"
    
    payload = {
        "description": description
    }
    
    try:
        response = requests.post(url, json=payload, timeout=30)
        
        if response.status_code == 200:
            data = response.json()
            print(f"\n✅ Prediction successful!")
            print(f"   Input: '{description[:100]}...'")
            print(f"   Label: {data['label']}")
            print(f"   Confidence: {data['confidence']}")
            print(f"   Model Version: {data['model_version']}")
            print(f"   Preprocessing Version: {data['preprocessing_version']}")
            
            # Verify consistency
            if data['model_version'] != data['preprocessing_version']:
                print(f"   ⚠️ Warning: Model and preprocessing versions don't match!")
            
            return data
        else:
            print(f"\n❌ Failed with status: {response.status_code}")
            print(f"   Response: {response.text[:200]}")
            return None
            
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
        return None


def run_comprehensive_tests():
    """Run all test scenarios"""
    
    # Test data based on actual training data categories
    test_samples = [
        {
            "description": "tomat merah berjamur putih dan kulit lembek berlendir di chiller 1",
            "category": "SPOILED"
        },
        {
            "description": "saus tomat kemasan botol expired date tercantum 12 September 2026 sudah terlampaui",
            "category": "EXPIRED"
        },
        {
            "description": "daun selada hijau segar jatuh ke lantai dapur yang kotor dan berminyak di area prep station",
            "category": "CONTAMINATED"
        },
        {
            "description": "daging steak sirloin terlalu lama digoreng di deep fryer sampai kering mengeras pahit",
            "category": "OVERCOOKED"
        },
        {
            "description": "kulit wortel dan bonggol brokoli sisa trimming di fish station",
            "category": "PREP_WASTE"
        },
        {
            "description": "nasi putih dan aneka lauk pauk sisa menu promo lunch siang yang berlebih tidak terjual",
            "category": "SURPLUS"
        },
        {
            "description": "potongan daging wagyu MB7 terkontaminasi serpihan plastik wrap kemasan tajam",
            "category": "CONTAMINATED"
        },
        {
            "description": "susu UHT full cream 1 liter tanggal kadaluarsa sudah habis saat pemeriksaan stok bulanan",
            "category": "EXPIRED"
        }
    ]
    
    print_section("COMPREHENSIVE WASTE PREDICTION TESTS")
    
    results = []
    
    for i, sample in enumerate(test_samples, 1):
        print(f"\n--- Test {i}/{len(test_samples)} ---")
        result = test_waste_prediction(
            sample["description"],
            sample["category"]
        )
        
        if result:
            results.append({
                "test": i,
                "input": sample["description"][:50],
                "predicted": result["label"],
                "confidence": result["confidence"],
                "expected": sample["category"]
            })
    
    # Summary
    print_section("TEST SUMMARY")
    print(f"\nTotal tests: {len(results)}")
    
    accurate = sum(1 for r in results if r["predicted"] == r["expected"])
    print(f"Accurate predictions: {accurate}/{len(results)} ({accurate/len(results)*100:.1f}%)")
    
    print("\nDetailed Results:")
    for r in results:
        status = "✅" if r["predicted"] == r["expected"] else "⚠️"
        print(f"{status} Test {r['test']}: Expected {r['expected']}, Got {r['predicted']} (conf: {r['confidence']})")
    
    return results


def test_error_handling():
    """Test error handling"""
    print_section("ERROR HANDLING TESTS")
    
    # Empty description
    print("\nTest: Empty description")
    try:
        response = requests.post(
            f"{BASE_URL}/api/ml/waste/predict",
            json={"description": ""},
            timeout=10
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
    except Exception as e:
        print(f"Error: {e}")
    
    # Missing field
    print("\nTest: Missing description field")
    try:
        response = requests.post(
            f"{BASE_URL}/api/ml/waste/predict",
            json={},
            timeout=10
        )
        print(f"Status: {response.status_code}")
        print(f"Response: {response.json()}")
    except Exception as e:
        print(f"Error: {e}")


def test_model_info():
    """Test model info endpoint"""
    print_section("MODEL INFO ENDPOINT")
    
    try:
        response = requests.get(f"{BASE_URL}/api/ml/models", timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            print("\nModel Information:")
            print(json.dumps(data, indent=2, ensure_ascii=False))
            return True
        else:
            print(f"❌ Failed: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


if __name__ == "__main__":
    print("=" * 70)
    print("KitchenGuard CSM - ML API Test Suite")
    print("=" * 70)
    
    # Wait a bit for server to be ready
    import time
    time.sleep(2)
    
    # Run tests
    health_ok = test_health_endpoint()
    
    if health_ok:
        test_results = run_comprehensive_tests()
        test_model_info()
        test_error_handling()
        
        print_section("TEST COMPLETE")
        print("\nAll tests finished successfully!")
    else:
        print("\n❌ Health check failed. Please start the server first.")
        print("   Run: python app/api/ml_rest_api.py")
