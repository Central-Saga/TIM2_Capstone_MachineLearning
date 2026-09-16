"""
Financial Loss Calculator for KitchenGuard CSM
Calculate monetary loss based on waste category and weight
"""

from typing import Dict, Optional
from datetime import datetime

# Cost model per kg (Rp) - Industry standard for commercial kitchen waste
# Based on market price of ingredients + disposal costs + environmental impact
WASTE_COST_PER_KG = {
    "CONTAMINATED": 150000,    # Highest - safety critical, includes health risk
    "SPOILED": 120000,          # High - spoiled ingredients expensive
    "EXPIRED": 100000,          # High - expired products lost value
    "OVERCOOKED": 80000,        # Medium-High - cooking labor + ingredients wasted
    "SURPLUS": 50000,           # Medium - unserved portions
    "PREP_WASTE": 20000         # Low - natural prep waste (peels, trimmings)
}

# Disposal cost multiplier by category (environmental impact factor)
DISPOSAL_MULTIPLIER = {
    "CONTAMINATED": 3.0,  # Hazardous waste disposal required
    "SPOILED": 2.5,       # Special handling needed
    "EXPIRED": 2.0,       # Standard disposal
    "OVERCOOKED": 1.5,    # Regular waste
    "SURPLUS": 1.2,       # Can be donated/composted
    "PREP_WASTE": 1.0     # Compostable naturally
}


def calculate_loss_per_category(category: str, weight_kg: float) -> Dict:
    """
    Calculate financial loss for a single waste entry
    
    Args:
        category: Waste category (CONTAMINATED, SPOILED, etc.)
        weight_kg: Weight in kilograms
    
    Returns:
        Dictionary with detailed loss breakdown
    """
    if category not in WASTE_COST_PER_KG:
        raise ValueError(f"Unknown category: {category}")
    
    if weight_kg <= 0:
        raise ValueError("Weight must be positive")
    
    # Base calculation
    base_cost_per_kg = WASTE_COST_PER_KG[category]
    disposal_factor = DISPOSAL_MULTIPLIER[category]
    
    # Calculate total loss
    ingredient_loss = base_cost_per_kg * weight_kg
    disposal_cost = ingredient_loss * (disposal_factor - 1) / disposal_factor
    total_loss = ingredient_loss + disposal_cost
    
    # Priority and recommendation
    priority_level = _get_priority_level(category)
    action_recommendation = _get_action_recommendation(category)
    
    return {
        "category": category,
        "weight_kg": round(weight_kg, 3),
        "cost_per_kg_rupiah": base_cost_per_kg,
        "disposal_factor": disposal_factor,
        "ingredient_loss_rupiah": round(ingredient_loss, 0),
        "disposal_cost_rupiah": round(disposal_cost, 0),
        "total_loss_rupiah": round(total_loss, 0),
        "priority_level": priority_level,
        "action_recommendation": action_recommendation,
        "timestamp": datetime.now().isoformat()
    }


def _get_priority_level(category: str) -> str:
    """Get priority level based on category"""
    priority_map = {
        "CONTAMINATED": "CRITICAL",
        "SPOILED": "HIGH", 
        "EXPIRED": "HIGH",
        "OVERCOOKED": "MEDIUM",
        "SURPLUS": "LOW",
        "PREP_WASTE": "LOW"
    }
    return priority_map.get(category, "UNKNOWN")


def _get_action_recommendation(category: str) -> str:
    """Get recommended action based on category"""
    recommendations = {
        "CONTAMINATED": "Dispose immediately - hazardous contamination",
        "SPOILED": "Document root cause - check storage conditions",
        "EXPIRED": "Review FIFO inventory system - prevent recurrence",
        "OVERCOOKED": "Retrain staff on cooking times/temperatures",
        "SURPLUS": "Consider portion adjustment or donate to food bank",
        "PREP_WASTE": "Optimize cutting techniques - minimize trim loss"
    }
    return recommendations.get(category, "Review and document")


def generate_daily_summary(waste_entries: list) -> Dict:
    """
    Generate daily loss summary from multiple waste entries
    
    Args:
        waste_entries: List of waste log entries with category and weight
    
    Returns:
        Comprehensive daily summary with breakdowns
    """
    if not waste_entries:
        return {"error": "No waste entries provided"}
    
    total_loss_rupiah = 0
    total_weight_kg = 0
    category_breakdown = {}
    priority_counts = {"CRITICAL": 0, "HIGH": 0, "MEDIUM": 0, "LOW": 0}
    
    for entry in waste_entries:
        category = entry.get("category")
        weight_kg = float(entry.get("weight_kg", 0))
        
        if category not in WASTE_COST_PER_KG:
            continue
            
        loss_data = calculate_loss_per_category(category, weight_kg)
        
        total_loss_rupiah += loss_data["total_loss_rupiah"]
        total_weight_kg += weight_kg
        
        # Category breakdown
        if category not in category_breakdown:
            category_breakdown[category] = {
                "count": 0,
                "total_weight_kg": 0,
                "total_loss_rupiah": 0
            }
        
        category_breakdown[category]["count"] += 1
        category_breakdown[category]["total_weight_kg"] += weight_kg
        category_breakdown[category]["total_loss_rupiah"] += loss_data["total_loss_rupiah"]
        
        # Priority counts
        priority_counts[loss_data["priority_level"]] += 1
    
    # Format category breakdown for output
    formatted_breakdown = {}
    for cat, data in category_breakdown.items():
        formatted_breakdown[cat] = {
            "count": data["count"],
            "total_weight_kg": round(data["total_weight_kg"], 3),
            "total_loss_rupiah": round(data["total_loss_rupiah"], 0)
        }
    
    return {
        "report_date": datetime.now().date().isoformat(),
        "summary": {
            "total_entries": len(waste_entries),
            "total_weight_kg": round(total_weight_kg, 3),
            "total_financial_loss_rupiah": round(total_loss_rupiah, 0),
            "average_loss_per_entry_rupiah": round(total_loss_rupiah / len(waste_entries), 0) if waste_entries else 0
        },
        "category_breakdown": formatted_breakdown,
        "priority_distribution": priority_counts,
        "risk_assessment": _assess_risk_level(priority_counts, total_loss_rupiah)
    }


def _assess_risk_level(priority_counts: Dict, total_loss: int) -> Dict:
    """Assess overall operational risk level"""
    critical_count = priority_counts.get("CRITICAL", 0)
    high_count = priority_counts.get("HIGH", 0)
    
    if critical_count > 0 or total_loss > 1000000:
        risk_level = "CRITICAL"
        message = "Immediate intervention required - critical safety incidents detected"
    elif high_count > 2 or total_loss > 500000:
        risk_level = "HIGH"
        message = "Elevated risk - review inventory and storage practices urgently"
    elif high_count > 0 or total_loss > 200000:
        risk_level = "MEDIUM"
        message = "Moderate risk - schedule process improvement review"
    else:
        risk_level = "LOW"
        message = "Acceptable risk level - continue monitoring"
    
    return {
        "level": risk_level,
        "message": message,
        "critical_incidents": critical_count,
        "high_priority_incidents": high_count
    }


if __name__ == "__main__":
    # Test calculations
    print("=" * 60)
    print("FINANCIAL LOSS CALCULATOR TEST")
    print("=" * 60)
    
    test_cases = [
        ("CONTAMINATED", 2.5),
        ("SPOILED", 1.2),
        ("EXPIRED", 3.0),
        ("OVERCOOKED", 0.8),
        ("SURPLUS", 5.0),
        ("PREP_WASTE", 1.5)
    ]
    
    for category, weight in test_cases:
        result = calculate_loss_per_category(category, weight)
        print(f"\n{category} ({weight} kg):")
        print(f"  Total Loss: Rp {result['total_loss_rupiah']:,.0f}")
        print(f"  Priority: {result['priority_level']}")
        print(f"  Action: {result['action_recommendation']}")
    
    print("\n" + "=" * 60)
    
    # Test daily summary
    print("\nDAILY SUMMARY TEST:")
    sample_entries = [
        {"category": "CONTAMINATED", "weight_kg": 1.5},
        {"category": "SPOILED", "weight_kg": 2.0},
        {"category": "OVERCOOKED", "weight_kg": 0.8},
        {"category": "PREP_WASTE", "weight_kg": 3.0}
    ]
    
    summary = generate_daily_summary(sample_entries)
    print(f"Total Entries: {summary['summary']['total_entries']}")
    print(f"Total Weight: {summary['summary']['total_weight_kg']} kg")
    print(f"Total Financial Loss: Rp {summary['summary']['total_financial_loss_rupiah']:,.0f}")
    print(f"Risk Level: {summary['risk_assessment']['level']}")
    print(f"Message: {summary['risk_assessment']['message']}")
