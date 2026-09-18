# Unit Test Suite - KitchenGuard CSM

Komponen testing comprehensive untuk sistem Machine Learning KitchenGuard CSM.

## 📁 Struktur Files

```
tests/
├── __init__.py              # Package initialization
├── test_cost_calculator.py  # Tests untuk financial loss calculator
└── test_kitchenguard.py     # Integration tests untuk semua fitur
```

## 🧪 Cara Menjalankan Tests

### Run Specific Test File:
```bash
# Test cost calculator module
python test_cost_calculator.py

# Test all features (if pytest available)
python test_kitchenguard.py
```

### Run All Tests:
```bash
cd tests
python -m pytest
```

## 📊 Coverage Tests

### ✅ test_cost_calculator.py
- **Constants Testing** - Verifikasi nilai cost dan multiplier
- **Loss Calculation** - Test perhitungan finansial per kategori
- **Error Handling** - Test edge cases (negatif, zero, invalid)
- **Priority Level** - Test assignment priority otomatis
- **Daily Summary** - Test agregasi dan laporan harian
- **Risk Assessment** - Test logika penentuan risk level

### 🚧 test_kitchenguard.py (Coming Soon)
- Waste classifier predictions
- Barcode service lookup
- Text preprocessing validation
- API endpoint integration

## 🔧 Menambah Test Baru

1. Buat test function/method baru dengan prefix `test_`
2. Gunakan assertion untuk verifikasi expected behavior
3. Jalankan test file langsung atau dengan pytest

Example:
```python
def test_new_feature(self):
    """Test for new feature"""
    result = calculate_loss_per_category('SPOILED', 2.0)
    assert result['total_loss_rupiah'] == 600000
```

## 📈 Test Coverage Metrics

| Module | Tests | Status |
|--------|-------|--------|
| Cost Calculator | 24 | ✅ Complete |
| Waste Classifier | TBD | 🚧 Planned |
| Barcode Service | TBD | 🚧 Planned |
| Preprocessing | TBD | 🚧 Planned |
| API Endpoints | TBD | 🚧 Planned |

## 🐛 Troubleshooting

### Issue: ModuleNotFoundError
**Solution:** Pastikan `src` directory ada di Python path
```python
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))
```

### Issue: Model files not found
**Solution:** Cek apakah model joblib sudah ada di `models/waste_classification/`

---

*Last Updated: September 17, 2026*  
*Maintained by: KitchenGuard CSM Development Team*
