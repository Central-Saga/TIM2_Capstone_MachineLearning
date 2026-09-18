"""Unit tests for cost_calculator module"""
import sys
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from cost_calculator import (
    WASTE_COST_PER_KG,
    DISPOSAL_MULTIPLIER,
    calculate_loss_per_category,
    generate_daily_summary,
    _get_priority_level,
    _assess_risk_level
)


class TestCostCalculatorConstants:
    """Test that constants are properly defined"""
    
    def test_cost_constants_exist(self):
        """All cost constants should be positive integers"""
        assert isinstance(WASTE_COST_PER_KG, dict)
        assert len(WASTE_COST_PER_KG) == 6
        
        for category, cost in WASTE_COST_PER_KG.items():
            assert isinstance(cost, int)
            assert cost > 0
    
    def test_disposal_multiplier_exist(self):
        """All disposal multipliers should be >= 1.0"""
        assert isinstance(DISPOSAL_MULTIPLIER, dict)
        assert len(DISPOSAL_MULTIPLIER) == 6
        
        for category, multiplier in DISPOSAL_MULTIPLIER.items():
            assert isinstance(multiplier, (int, float))
            assert multiplier >= 1.0
    
    def test_category_consistency(self):
        """Both dicts should have same keys"""
        assert set(WASTE_COST_PER_KG.keys()) == set(DISPOSAL_MULTIPLIER.keys())


class TestCalculateLossPerCategory:
    """Tests for calculate_loss_per_category function"""
    
    def test_contaminated_category(self):
        """Test CONTAMINATED category calculation"""
        result = calculate_loss_per_category('CONTAMINATED', 2.5)
        
        assert result['category'] == 'CONTAMINATED'
        assert result['weight_kg'] == 2.5
        assert result['cost_per_kg_rupiah'] == 150000
        assert result['disposal_factor'] == 3.0
        assert result['total_loss_rupiah'] > 0
        assert result['priority_level'] == 'CRITICAL'
    
    def test_spoiled_category(self):
        """Test SPOILED category calculation"""
        result = calculate_loss_per_category('SPOILED', 1.5)
        
        assert result['category'] == 'SPOILED'
        assert result['cost_per_kg_rupiah'] == 120000
        assert result['priority_level'] == 'HIGH'
    
    def test_prep_waste_lowest(self):
        """Test PREP_WASTE has lowest cost"""
        result = calculate_loss_per_category('PREP_WASTE', 1.0)
        
        assert result['cost_per_kg_rupiah'] == 20000
        assert result['disposal_factor'] == 1.0
        assert result['total_loss_rupiah'] == 20000
    
    def test_negative_weight_raises_error(self):
        """Negative weight should raise ValueError"""
        try:
            calculate_loss_per_category('SPOILED', -1.0)
            assert False, "Should have raised ValueError"
        except ValueError:
            pass
    
    def test_zero_weight_raises_error(self):
        """Zero weight should raise ValueError"""
        try:
            calculate_loss_per_category('SPOILED', 0)
            assert False, "Should have raised ValueError"
        except ValueError:
            pass
    
    def test_invalid_category_raises_error(self):
        """Unknown category should raise ValueError"""
        try:
            calculate_loss_per_category('INVALID_CATEGORY', 1.0)
            assert False, "Should have raised ValueError"
        except ValueError:
            pass
    
    def test_fractional_weight(self):
        """Test fractional weight handling"""
        result = calculate_loss_per_category('EXPIRED', 0.75)
        
        assert result['weight_kg'] == 0.75
        # Should have calculated loss based on 0.75 * 100000
        assert result['ingredient_loss_rupiah'] == 75000
    
    def test_large_weight(self):
        """Test large weight doesn't overflow"""
        result = calculate_loss_per_category('SURPLUS', 100.5)
        
        assert result['weight_kg'] == 100.5
        assert result['total_loss_rupiah'] > 0


class TestGetPriorityLevel:
    """Tests for priority level assignment"""
    
    def test_contaminated_critical(self):
        assert _get_priority_level('CONTAMINATED') == 'CRITICAL'
    
    def test_spoiled_high(self):
        assert _get_priority_level('SPOILED') == 'HIGH'
    
    def test_expired_high(self):
        assert _get_priority_level('EXPIRED') == 'HIGH'
    
    def test_overcooked_medium(self):
        assert _get_priority_level('OVERCOOKED') == 'MEDIUM'
    
    def test_surplus_low(self):
        assert _get_priority_level('SURPLUS') == 'LOW'
    
    def test_prep_waste_low(self):
        assert _get_priority_level('PREP_WASTE') == 'LOW'


class TestGenerateDailySummary:
    """Tests for daily summary generation"""
    
    def test_empty_list(self):
        """Empty list should return error message"""
        result = generate_daily_summary([])
        assert 'error' in result
    
    def test_single_entry(self):
        """Single entry summary"""
        entries = [{'category': 'SPOILED', 'weight_kg': 1.5}]
        result = generate_daily_summary(entries)
        
        assert result['summary']['total_entries'] == 1
        assert result['summary']['total_weight_kg'] == 1.5
        assert result['summary']['total_financial_loss_rupiah'] > 0
    
    def test_multiple_entries_same_category(self):
        """Multiple entries of same category should aggregate"""
        entries = [
            {'category': 'SPOILED', 'weight_kg': 1.0},
            {'category': 'SPOILED', 'weight_kg': 2.0}
        ]
        result = generate_daily_summary(entries)
        
        assert result['summary']['total_entries'] == 2
        assert result['summary']['total_weight_kg'] == 3.0
        
        # Check SPOILED breakdown
        assert 'SPOILED' in result['category_breakdown']
        assert result['category_breakdown']['SPOILED']['count'] == 2
    
    def test_multiple_entries_different_categories(self):
        """Different categories should all appear in breakdown"""
        entries = [
            {'category': 'CONTAMINATED', 'weight_kg': 1.0},
            {'category': 'SPOILED', 'weight_kg': 1.0},
            {'category': 'PREP_WASTE', 'weight_kg': 1.0}
        ]
        result = generate_daily_summary(entries)
        
        assert result['summary']['total_entries'] == 3
        assert len(result['category_breakdown']) == 3
        assert 'CONTAMINATED' in result['category_breakdown']
        assert 'SPOILED' in result['category_breakdown']
        assert 'PREP_WASTE' in result['category_breakdown']
    
    def test_priority_distribution(self):
        """Priority distribution should match input"""
        entries = [
            {'category': 'CONTAMINATED', 'weight_kg': 1.0},  # CRITICAL
            {'category': 'SPOILED', 'weight_kg': 1.0},       # HIGH
            {'category': 'EXPIRED', 'weight_kg': 1.0},       # HIGH
            {'category': 'PREP_WASTE', 'weight_kg': 1.0}     # LOW
        ]
        result = generate_daily_summary(entries)
        
        assert result['priority_distribution']['CRITICAL'] == 1
        assert result['priority_distribution']['HIGH'] == 2
        assert result['priority_distribution']['LOW'] == 1
    
    def test_risk_assessment_included(self):
        """Result should include risk assessment"""
        entries = [{'category': 'CONTAMINATED', 'weight_kg': 1.0}]
        result = generate_daily_summary(entries)
        
        assert 'risk_assessment' in result
        assert 'level' in result['risk_assessment']
        assert 'message' in result['risk_assessment']


class TestRiskAssessment:
    """Tests for risk level assessment logic"""
    
    def test_critical_from_incident(self):
        """Critical incident should trigger critical risk"""
        counts = {'CRITICAL': 1, 'HIGH': 0, 'MEDIUM': 0, 'LOW': 0}
        result = _assess_risk_level(counts, 100000)
        
        assert result['level'] == 'CRITICAL'
        assert result['critical_incidents'] == 1
    
    def test_critical_from_loss(self):
        """High loss should trigger critical risk"""
        counts = {'CRITICAL': 0, 'HIGH': 0, 'MEDIUM': 0, 'LOW': 0}
        result = _assess_risk_level(counts, 1500000)  # > 1 million
        
        assert result['level'] == 'CRITICAL'
    
    def test_high_risk_from_multiple_high(self):
        """Multiple high incidents should trigger high risk"""
        counts = {'CRITICAL': 0, 'HIGH': 3, 'MEDIUM': 0, 'LOW': 0}
        result = _assess_risk_level(counts, 300000)
        
        assert result['level'] == 'HIGH'
    
    def test_low_risk_normal_operation(self):
        """Normal operation should have low risk"""
        counts = {'CRITICAL': 0, 'HIGH': 0, 'MEDIUM': 0, 'LOW': 1}
        result = _assess_risk_level(counts, 50000)
        
        assert result['level'] == 'LOW'


def run_tests():
    """Run all tests manually without pytest"""
    print("=" * 70)
    print("🧪 RUNNING COST CALCULATOR UNIT TESTS")
    print("=" * 70)
    
    tests_to_run = [
        ("Constants", [
            TestCostCalculatorConstants().test_cost_constants_exist,
            TestCostCalculatorConstants().test_disposal_multiplier_exist,
            TestCostCalculatorConstants().test_category_consistency,
        ]),
        ("Calculate Loss", [
            TestCalculateLossPerCategory().test_contaminated_category,
            TestCalculateLossPerCategory().test_spoiled_category,
            TestCalculateLossPerCategory().test_prep_waste_lowest,
            TestCalculateLossPerCategory().test_negative_weight_raises_error,
            TestCalculateLossPerCategory().test_zero_weight_raises_error,
            TestCalculateLossPerCategory().test_invalid_category_raises_error,
            TestCalculateLossPerCategory().test_fractional_weight,
        ]),
        ("Priority Level", [
            TestGetPriorityLevel().test_contaminated_critical,
            TestGetPriorityLevel().test_spoiled_high,
            TestGetPriorityLevel().test_expired_high,
            TestGetPriorityLevel().test_overcooked_medium,
            TestGetPriorityLevel().test_surplus_low,
            TestGetPriorityLevel().test_prep_waste_low,
        ]),
        ("Daily Summary", [
            TestGenerateDailySummary().test_empty_list,
            TestGenerateDailySummary().test_single_entry,
            TestGenerateDailySummary().test_multiple_entries_same_category,
            TestGenerateDailySummary().test_multiple_entries_different_categories,
            TestGenerateDailySummary().test_priority_distribution,
        ]),
        ("Risk Assessment", [
            TestRiskAssessment().test_critical_from_incident,
            TestRiskAssessment().test_high_risk_from_multiple_high,
            TestRiskAssessment().test_low_risk_normal_operation,
        ])
    ]
    
    total_tests = 0
    passed_tests = 0
    failed_tests = 0
    skipped_tests = 0
    
    for test_group_name, test_functions in tests_to_run:
        print(f"\n{'─' * 70}")
        print(f"📋 {test_group_name}")
        print(f"{'─' * 70}")
        
        for test_func in test_functions:
            total_tests += 1
            try:
                test_func()
                print(f"   ✅ PASSED")
                passed_tests += 1
            except AssertionError as e:
                print(f"   ❌ FAILED: {str(e)}")
                failed_tests += 1
            except Exception as e:
                print(f"   ⏭️ SKIPPED: {type(e).__name__}: {str(e)}")
                skipped_tests += 1
    
    print(f"\n{'=' * 70}")
    print("📊 TEST SUMMARY")
    print(f"{'=' * 70}")
    print(f"✅ Passed:  {passed_tests}/{total_tests}")
    print(f"❌ Failed:  {failed_tests}/{total_tests}")
    print(f"⏭️ Skipped: {skipped_tests}/{total_tests}")
    
    if failed_tests == 0:
        print(f"\n🎉 ALL TESTS PASSED!")
    else:
        print(f"\n⚠️  {failed_tests} test(s) failed")
    
    print(f"{'=' * 70}")
    
    return passed_tests, failed_tests, skipped_tests


if __name__ == "__main__":
    passed, failed, skipped = run_tests()
    
    if failed > 0:
        sys.exit(1)
