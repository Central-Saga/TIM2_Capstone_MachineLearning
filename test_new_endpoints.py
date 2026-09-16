"""
Test Script for New KitchenGuard CSM v3.0 Endpoints
Tests all financial calculator and report endpoints
"""

import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:8000"

def print_section(title):
    print("\n" + "=" * 60)
    print(f"  {title}")
    print("=" * 60)


def test_health_check():
    """Test health check endpoint"""
    print_section("TEST 1: Health Check")
    
    try:
        response = requests.get(f"{BASE_URL}/api/health")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Status: {data['status']}")
            print(f"✓ Service: {data['service']}")
            print(f"✓ Version: {data['version']}")
            print(f"✓ Model Loaded: {data['model_loaded']}")
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Error: {e}")
        return False


def test_financial_calculator():
    """Test financial loss calculation endpoint"""
    print_section("TEST 2: Financial Loss Calculator")
    
    # Test each category with different weights
    test_cases = [
        {"category": "CONTAMINATED", "weight_kg": 1.5},
        {"category": "SPOILED", "weight_kg": 2.5},
        {"category": "EXPIRED", "weight_kg": 3.0},
        {"category": "OVERCOOKED", "weight_kg": 0.8},
        {"category": "SURPLUS", "weight_kg": 5.0},
        {"category": "PREP_WASTE", "weight_kg": 1.2}
    ]
    
    success_count = 0
    
    for test in test_cases:
        try:
            response = requests.post(
                f"{BASE_URL}/api/calculate-loss",
                json=test
            )
            
            if response.status_code == 200:
                data = response.json()
                result = data['data']
                
                print(f"\n{test['category']} ({test['weight_kg']} kg):")
                print(f"  Cost per kg: Rp {result['cost_per_kg_rupiah']:,.0f}")
                print(f"  Total Loss:  Rp {result['total_loss_rupiah']:,.0f}")
                print(f"  Priority:    {result['priority_level']}")
                print(f"  Action:      {result['action_recommendation']}")
                success_count += 1
            else:
                print(f"✗ {test['category']}: Failed - {response.status_code}")
                
        except Exception as e:
            print(f"✗ Error testing {test['category']}: {e}")
    
    print(f"\n✓ Success: {success_count}/{len(test_cases)} categories tested")
    return success_count == len(test_cases)


def test_daily_report():
    """Test daily summary report endpoint"""
    print_section("TEST 3: Daily Waste Summary Report")
    
    # Create sample waste entries
    sample_entries = [
        {"category": "CONTAMINATED", "weight_kg": 1.5},
        {"category": "SPOILED", "weight_kg": 2.0},
        {"category": "SPOILED", "weight_kg": 1.0},
        {"category": "EXPIRED", "weight_kg": 3.5},
        {"category": "OVERCOOKED", "weight_kg": 0.8},
        {"category": "OVERCOOKED", "weight_kg": 0.5},
        {"category": "SURPLUS", "weight_kg": 4.0},
        {"category": "PREP_WASTE", "weight_kg": 2.5}
    ]
    
    try:
        response = requests.post(
            f"{BASE_URL}/api/reports/daily-summary",
            json={"waste_entries": sample_entries}
        )
        
        if response.status_code == 200:
            data = response.json()
            report = data['data']
            
            print(f"\nReport Date: {report['report_date']}")
            print(f"\nSummary:")
            print(f"  Total Entries: {report['summary']['total_entries']}")
            print(f"  Total Weight:  {report['summary']['total_weight_kg']} kg")
            print(f"  Total Loss:    Rp {report['summary']['total_financial_loss_rupiah']:,.0f}")
            print(f"  Avg per Entry: Rp {report['summary']['average_loss_per_entry_rupiah']:,.0f}")
            
            print(f"\nCategory Breakdown:")
            for cat, stats in report['category_breakdown'].items():
                print(f"  {cat}:")
                print(f"    Count: {stats['count']}")
                print(f"    Weight: {stats['total_weight_kg']} kg")
                print(f"    Loss: Rp {stats['total_loss_rupiah']:,.0f}")
            
            risk = report['risk_assessment']
            print(f"\nRisk Assessment:")
            print(f"  Level: {risk['level']}")
            print(f"  Message: {risk['message']}")
            print(f"  Critical Incidents: {risk['critical_incidents']}")
            print(f"  High Priority: {risk['high_priority_incidents']}")
            
            return True
        else:
            print(f"✗ Failed: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"✗ Error: {e}")
        return False


def test_classification_with_loss():
    """Test ML classification with integrated loss calculation"""
    print_section("TEST 4: ML Classification + Financial Loss")
    
    test_texts = [
        {
            "text": "Daging sapi tenderloin berbau busuk asam dan berlendir",
            "estimated_weight": 2.0
        },
        {
            "text": "Saus mayonnaise botol sudah lewat expired date seminggu lalu",
            "estimated_weight": 0.5
        },
        {
            "text": "Kulit apel dan biji melon kupasan persiapan fruit salad",
            "estimated_weight": 1.0
        }
    ]
    
    for i, test in enumerate(test_texts, 1):
        try:
            response = requests.post(
                f"{BASE_URL}/api/predict",
                json={
                    "text": test["text"],
                    "threshold": 0.85,
                    "estimated_weight_kg": test["estimated_weight"]
                }
            )
            
            if response.status_code == 200:
                data = response.json()
                ai = data['ai']
                
                print(f"\nTest {i}:")
                print(f"Input: {test['text']}")
                print(f"\nClassification:")
                print(f"  Category: {ai['predicted_class']}")
                print(f"  Confidence: {ai['confidence']:.2%}")
                print(f"  Gate Status: {ai['gate_status']}")
                print(f"  Action: {ai['action_recommendation']}")
                
                if ai.get('financial_impact'):
                    fi = ai['financial_impact']
                    print(f"\nFinancial Impact:")
                    print(f"  Weight: {fi['weight_kg']} kg")
                    print(f"  Total Loss: Rp {fi['total_loss_rupiah']:,.0f}")
                    
            else:
                print(f"✗ Test {i} failed: {response.status_code}")
                
        except Exception as e:
            print(f"✗ Error in test {i}: {e}")


def main():
    print("\n" + "#" * 60)
    print("#  KitchenGuard CSM v3.0 - API Endpoints Test Suite")
    print("#  Testing Financial Calculator & Reports Features")
    print("#" * 60)
    
    results = []
    
    # Test 1: Health Check
    results.append(("Health Check", test_health_check()))
    
    # Test 2: Financial Calculator
    results.append(("Financial Calculator", test_financial_calculator()))
    
    # Test 3: Daily Report
    results.append(("Daily Report", test_daily_report()))
    
    # Test 4: Classification with Loss
    test_classification_with_loss()
    
    # Summary
    print("\n" + "=" * 60)
    print("  TEST SUMMARY")
    print("=" * 60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status} - {name}")
    
    print(f"\nTotal: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All critical endpoints working correctly!")
        print("\nNext steps:")
        print("1. Start Android development using Retrofit (see IMPLEMENTATION_GUIDE_V3.md)")
        print("2. Test voice input integration")
        print("3. Deploy to production environment")
    else:
        print("\n⚠️ Some tests failed. Please check the server logs.")
    
    print("\n" + "#" * 60)


if __name__ == "__main__":
    # Check if server is running
    try:
        requests.get(f"{BASE_URL}/api/health", timeout=2)
        main()
    except requests.exceptions.ConnectionError:
        print("\n⚠️  Server not running at http://localhost:8000")
        print("\nTo start the server:")
        print("  python app.py")
        print("\nOr:")
        print("  uvicorn app:app --host 127.0.0.1 --port 8000")
        print("\nWaiting 5 seconds to start server...")
        import time
        time.sleep(5)
        # Try to start server automatically
        import subprocess
        subprocess.Popen(["python", "app.py"])
        time.sleep(3)
        main()
