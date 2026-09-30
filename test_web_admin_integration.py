#!/usr/bin/env python
"""
Test script for Web Admin waste classification integration
Tests the full flow: Web -> Backend Gateway -> ML FastAPI
"""

import requests
import json
from datetime import datetime

BACKEND_URL = "http://localhost:3001"

def test_prediction(description, expected_category):
    """Test single prediction via backend gateway"""
    url = f"{BACKEND_URL}/api/ml/waste/predict"
    
    payload = {"description": description}
    
    start = datetime.now()
    response = requests.post(url, json=payload, timeout=30)
    elapsed = (datetime.now() - start).total_seconds() * 1000
    
    if response.status_code == 200:
        data = response.json()
        
        correct = data["label"].upper() == expected_category.upper()
        
        print(f"   Input: \"{description[:60]}...\"")
        print(f"   Confidence: {data['confidence']:.4f}")
        print(f"   Time: {elapsed:.2f}ms")
        
        return {
            "category": expected_category,
            "predicted": data["label"],
            "correct": correct,
            "time": elapsed
        }
    else:
        print(f"❌ HTTP {response.status_code}")
        print(f"   Response: {response.text[:100]}")
        
        return {"category": expected_category, "error": f"HTTP {response.status_code}"}

print("="*80)
print("Web Admin Integration Test")
print("Flow: Web Component -> Backend Gateway :3001 -> ML FastAPI :8000")
print("="*80)

# Run tests
results = []

tests = [
    ("SPOILED", "ayam mulai berlendir dan berbau tidak sedap di chiller"),
    ("EXPIRED", "saus tomat kemasan expired date sudah terlewati seminggu"),
    ("CONTAMINATED", "daun selada jatuh ke lantai dapur yang kotor"),
    ("OVERCOOKED", "daging steak terlalu lama digoreng sampai kering pahit"),
    ("PREP_WASTE", "kulit wortel sisa trimming preparation station"),
    ("SURPLUS", "nasi putih sisa menu promo lunch siang tidak terjual"),
]

for expected, description in tests:
    result = test_prediction(description, expected)
    results.append(result)

# Summary
print("\n" + "="*80)
print("RESULTS SUMMARY")
print("="*80)

passed = sum(1 for r in results if r.get("correct", False))
total = len(results)

print(f"\nTests: {passed}/{total} passed ({passed/total*100:.1f}%)")

if passed == total:
    print("\n🎉 ALL TESTS PASSED!")
    print("Web Admin integration is working correctly.")
else:
    print(f"\n⚠️  {total - passed} tests failed")

print("="*80)

# Save results
with open("web_admin_integration_results.json", "w") as f:
    json.dump({
        "timestamp": datetime.now().isoformat(),
        "backend_url": BACKEND_URL,
        "tests_run": total,
        "passed": passed,
        "results": results
    }, f, indent=2)

print(f"Results saved to: web_admin_integration_results.json")
