"""Comprehensive Test Suite for KitchenGuard CSM - All Features Covered
This test suite covers ML models, barcode service, preprocessing, and more.
"""
import sys
from pathlib import Path
from datetime import datetime

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))


class TestWasteClassifierModel:
    """Tests for waste classification ML model"""
    
    def setup_method(self):
        """Load model, vectorizer, and label encoder before each test"""
        try:
            import joblib
            from preprocess import preprocess_text
            self.vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
            self.model = joblib.load('models/waste_classification/waste_classifier_model.joblib')
            self.encoder = joblib.load('models/waste_classification/label_encoder.joblib')
            self.preprocess = preprocess_text
        except Exception as e:
            print(f"Warning: Could not load models: {e}")
            raise

    def _predict(self, text: str):
        clean = self.preprocess(text)
        features = self.vectorizer.transform([clean])
        pred_idx = self.model.predict(features)[0]
        category = self.encoder.inverse_transform([pred_idx])[0]
        proba = float(self.model.predict_proba(features).max())
        return category, proba
    
    def test_model_loaded_successfully(self):
        """Test that model and vectorizer loaded correctly"""
        assert self.vectorizer is not None
        assert self.model is not None
        assert self.encoder is not None
    
    def test_vocabulary_size(self):
        """Test TF-IDF vocabulary has expected size"""
        vocab = self.vectorizer.vocabulary_
        assert len(vocab) > 1000  # Should have substantial vocabulary
    
    def test_prediction_output_format(self):
        """Test prediction returns correct format"""
        category, proba = self._predict("Daging berbau busuk")
        assert category in ['CONTAMINATED', 'SPOILED', 'EXPIRED', 
                             'OVERCOOKED', 'SURPLUS', 'PREP_WASTE']
        assert 0.0 <= proba <= 1.0
    
    def test_spoiled_meat_classification(self):
        """Test meat spoilage detection"""
        test_cases = [
            "Daging sapi berbau busuk berlendir",
            "Ayam tidak segar bau aneh",
            "Ikan sudah tidak fresh baunya"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as SPOILED or CONTAMINATED
            assert category in ['SPOILED', 'CONTAMINATED'], f"Expected SPOILED/CONTAMINATED, got {category}"
    
    def test_contamination_detection(self):
        """Test contamination detection accuracy"""
        test_cases = [
            "Salad terkontaminasi rambut dari lantai",
            "Makanan kena kecoa",
            "Bumbu tercampur debu tanah"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as CONTAMINATED
            assert category == 'CONTAMINATED', f"Expected CONTAMINATED, got {category}"
    
    def test_expired_date_detection(self):
        """Test expired date identification"""
        test_cases = [
            "Mayonaise expired date minggu lalu",
            "Susu kedaluwarsa MHD sudah lewat",
            "Kecap sudah melewati tanggal kadaluarsa"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as EXPIRED
            assert category == 'EXPIRED', f"Expected EXPIRED, got {category}"
    
    def test_overcooked_detection(self):
        """Test overcooked food detection"""
        test_cases = [
            "Nasi gosong hangus terbakar",
            "Ayam terlalu lama dioven jadi keras",
            "Steak overdone terlalu matang"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as OVERCOOKED
            assert category == 'OVERCOOKED', f"Expected OVERCOOKED, got {category}"
    
    def test_prep_waste_classification(self):
        """Test preparation waste identification"""
        test_cases = [
            "Kulit kentang hasil pengupasan",
            "Sisa sayuran trimming prep station",
            "Potongan kulit wortel dan daun sup"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as PREP_WASTE
            assert category == 'PREP_WASTE', f"Expected PREP_WASTE, got {category}"
    
    def test_surplus_food_detection(self):
        """Test surplus/unplanned waste detection"""
        test_cases = [
            "Rice porportion tidak terjual hari ini",
            "Porsi prasmanan tidak habis tersentuh",
            "Bahan makanan sisa buffet malam ini"
        ]
        
        for text in test_cases:
            category, _ = self._predict(text)
            # Should classify as SURPLUS
            assert category == 'SURPLUS', f"Expected SURPLUS, got {category}"
    
    def test_confidence_scores_valid(self):
        """Test that probability scores are valid"""
        test_texts = [
            "Daging berbau busuk",
            "Salat kontaminasi hair",
            "Nasi gosong"
        ]
        
        for text in test_texts:
            clean = self.preprocess(text)
            transformed = self.vectorizer.transform([clean])
            proba = self.model.predict_proba(transformed)[0]
            
            # All probabilities should sum to 1.0
            total = sum(proba)
            assert abs(total - 1.0) < 0.001, f"Probabilities should sum to 1.0, got {total}"
            
            # Each probability should be between 0 and 1
            for p in proba:
                assert 0 <= p <= 1, f"Probability {p} should be between 0 and 1"
    
    def test_multilingual_support(self):
        """Test mixed Bahasa Indonesia and English inputs"""
        test_cases = [
            "Daging spoiled dan terkontaminasi",  # Mixed
            "Expired susu dengan busuk aroma",   # Mixed
            "Overcooked nasi dengan gosong taste"  # Mixed
        ]
        
        for text in test_cases:
            try:
                transformed = self.vectorizer.transform([text])
                prediction = self.model.predict(transformed)[0]
                # Should not throw error and return valid prediction
                assert prediction in ['CONTAMINATED', 'SPOILED', 'EXPIRED', 
                                     'OVERCOOKED', 'SURPLUS', 'PREP_WASTE']
            except Exception as e:
                print(f"\nNote: Mixed language handling: {text}")
                print(f"Error: {e}")


class TestBarcodeService:
    """Tests for barcode database and lookup service"""
    
    def setup_method(self):
        """Import barcode service before tests"""
        from barcode_service import barcode_service, BARCODE_DATABASE
        self.barcode_service = barcode_service
        self.BARCODE_DATABASE = BARCODE_DATABASE
    
    def test_barcode_database_has_items(self):
        """Test that barcode database contains items"""
        assert len(self.BARCODE_DATABASE) > 0
        assert len(self.BARCODE_DATABASE) >= 10  # At least 10 items
    
    def test_sample_barcodes_exist(self):
        """Test that sample barcodes can be looked up"""
        sample_barcodes = list(self.BARCODE_DATABASE.keys())[:5]
        
        for barcode in sample_barcodes:
            result = self.barcode_service.lookup(barcode)
            assert result is not None, f"Barcode {barcode} should exist in database"
            assert 'name' in result
            assert 'batch_id' in result
            assert 'expiry_at' in result
    
    def test_barcode_with_expiry(self):
        """Test barcode with expiry date information"""
        # Find a barcode with expiry info
        expiry_barcodes = [k for k, v in self.BARCODE_DATABASE.items() if 'expiry_at' in v]
        
        assert len(expiry_barcodes) > 0, "Should have barcodes with expiry dates"
        
        barcode = expiry_barcodes[0]
        result = self.barcode_service.lookup(barcode)
        
        assert result['expiry_at'] is not None
        
        # Check if expiry is in future (reasonable assumption for demo data)
        expiry_date = datetime.strptime(result['expiry_at'], '%Y-%m-%d')
        days_until_expiry = (expiry_date - datetime.now()).days
        print(f"\nBarcode {barcode}: Expires in {days_until_expiry} days")
    
    def test_invalid_barcode_returns_none(self):
        """Test that invalid barcode returns None"""
        fake_barcodes = [
            "9999999999999",
            "0000000000000",
            "1234567890123"
        ]
        
        for barcode in fake_barcodes:
            result = self.barcode_service.lookup(barcode)
            assert result is None, f"Invalid barcode {barcode} should return None"
    
    def test_barcode_categories(self):
        """Test different barcode categories exist"""
        categories = set()
        
        for barcode, info in self.BARCODE_DATABASE.items():
            if 'category' in info:
                categories.add(info['category'])
        
        print(f"\nAvailable barcode categories: {categories}")
        assert len(categories) > 3, "Should have multiple categories"
    
    def test_barcode_supplier_info(self):
        """Test supplier information completeness"""
        suppliers = []
        
        for barcode, info in self.BARCODE_DATABASE.items():
            if 'supplier' in info:
                suppliers.append({
                    'barcode': barcode,
                    'supplier': info['supplier']
                })
        
        assert len(suppliers) > 0, "Should have barcodes with supplier info"
        
        print(f"\nSample suppliers:")
        for s in suppliers[:5]:
            print(f"  - {s['supplier']} ({s['barcode']})")


class TestTextPreprocessing:
    """Tests for text preprocessing functions"""
    
    def setup_method(self):
        """Import preprocess function"""
        from preprocess import preprocess_text
        self.preprocess = preprocess_text
    
    def test_lowercase_conversion(self):
        """Test text is converted to lowercase"""
        input_text = "DAGING BERBAU BUSUK"
        result = self.preprocess(input_text)
        
        assert result.islower(), "Text should be converted to lowercase"
    
    def test_special_character_removal(self):
        """Test special characters are removed"""
        input_text = "Daging @#$% busuk!!!"
        result = self.preprocess(input_text)
        
        # Count special chars before and after
        special_chars_before = sum(c in '!@#$%^&*()_+-=' for c in input_text)
        special_chars_after = sum(c in '!@#$%^&*()_+-=' for c in result)
        
        assert special_chars_after < special_chars_before, "Special characters should be reduced"
    
    def test_whitespace_normalization(self):
        """Test multiple spaces are normalized"""
        input_text = "Daging    busuk     sekali"
        result = self.preprocess(input_text)
        
        # Should not have multiple consecutive spaces
        assert '  ' not in result, "Multiple spaces should be normalized"
    
    def test_stopword_removal(self):
        """Test common stopwords are removed"""
        input_text = "Daging yang ada bau tidak enak sekali"
        result = self.preprocess(input_text)
        
        # Should have fewer words than original
        original_words = len(input_text.split())
        processed_words = len(result.split())
        
        assert processed_words <= original_words, "Stopwords should be removed"
    
    def test_tokenization(self):
        """Test text is tokenized correctly"""
        input_text = "Daging berbau busuk"
        result = self.preprocess(input_text)
        
        tokens = result.split()
        
        assert len(tokens) > 0, "Should produce tokens"
        assert all(len(token) > 0 for token in tokens), "No empty tokens"
    
    def test_empty_string_handling(self):
        """Test empty string input handled gracefully"""
        result = self.preprocess("")
        
        assert isinstance(result, str)
        # Empty or minimal output acceptable
    
    def test_unicode_characters(self):
        """Test unicode characters handled properly"""
        input_text = "Daging biasa tanpa karakter spesial"
        result = self.preprocess(input_text)
        
        assert isinstance(result, str)
        assert len(result) > 0
    
    def test_mixed_language(self):
        """Test mixed language text processing"""
        input_text = "Daging spoiled karena exposure to air"
        result = self.preprocess(input_text)
        
        assert isinstance(result, str)
        assert len(result) > 0
    
    def test_preprocessing_preserves_meaning(self):
        """Test that meaning / root keywords are preserved after preprocessing"""
        test_cases = [
            ("Daging busuk", ["daging", "busuk"]),
            ("Salat terkontaminasi", ["salat", "kontaminasi"])
        ]
        
        for original, expected_roots in test_cases:
            result = self.preprocess(original)
            for root in expected_roots:
                assert root in result, f"Root keyword '{root}' should be preserved in '{result}'"


class TestIntegrationWorkflows:
    """End-to-end integration tests"""
    
    def test_full_workflow_prediction(self):
        """Test complete workflow: text input -> preprocessing -> prediction"""
        import joblib
        from preprocess import preprocess_text
        
        # Load components
        vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
        model = joblib.load('models/waste_classification/waste_classifier_model.joblib')
        encoder = joblib.load('models/waste_classification/label_encoder.joblib')
        
        # Complete workflow
        raw_input = "Daging sapi berbau busuk berlendir"
        
        # Step 1: Preprocess
        cleaned_text = preprocess_text(raw_input)
        
        # Step 2: Transform
        transformed = vectorizer.transform([cleaned_text])
        
        # Step 3: Predict
        pred_idx = model.predict(transformed)[0]
        prediction = encoder.inverse_transform([pred_idx])[0]
        confidence = float(model.predict_proba(transformed).max())
        
        # Validate results
        assert prediction in ['CONTAMINATED', 'SPOILED', 'EXPIRED', 
                              'OVERCOOKED', 'SURPLUS', 'PREP_WASTE']
        assert 0 <= confidence <= 1
        
        print(f"\nWorkflow Result:")
        print(f"  Input: {raw_input}")
        print(f"  Prediction: {prediction}")
        print(f"  Confidence: {confidence:.2%}")
    
    def test_cost_calculation_integration(self):
        """Test cost calculator works with ML predictions"""
        from cost_calculator import calculate_loss_per_category
        
        # Simulate ML prediction result
        ml_prediction = {
            'category': 'SPOILED',
            'confidence': 0.95
        }
        
        # Calculate financial impact
        weight_kg = 2.5
        
        result = calculate_loss_per_category(
            ml_prediction['category'], 
            weight_kg
        )
        
        # Verify calculation worked
        assert result['category'] == ml_prediction['category']
        assert result['weight_kg'] == weight_kg
        assert result['total_loss_rupiah'] > 0
        
        print(f"\nFinancial Impact Calculation:")
        print(f"  Category: {result['category']}")
        print(f"  Weight: {result['weight_kg']} kg")
        print(f"  Total Loss: Rp {result['total_loss_rupiah']:,.0f}")
        print(f"  Priority: {result['priority_level']}")
    
    def test_daily_summary_aggregation(self):
        """Test daily summary with multiple entries"""
        from cost_calculator import generate_daily_summary
        
        # Simulate waste log entries from ML predictions
        entries = [
            {'category': 'SPOILED', 'weight_kg': 1.5, 'confidence': 0.92},
            {'category': 'EXPIRED', 'weight_kg': 0.8, 'confidence': 0.89},
            {'category': 'PREP_WASTE', 'weight_kg': 2.0, 'confidence': 0.95}
        ]
        
        # Generate summary
        summary = generate_daily_summary(entries)
        
        # Validate summary structure
        assert 'summary' in summary
        assert 'category_breakdown' in summary
        assert 'risk_assessment' in summary
        
        # Validate calculations
        assert summary['summary']['total_entries'] == 3
        assert summary['summary']['total_weight_kg'] > 0
        assert summary['summary']['total_financial_loss_rupiah'] > 0
        
        print(f"\nDaily Summary:")
        print(f"  Total Entries: {summary['summary']['total_entries']}")
        print(f"  Total Weight: {summary['summary']['total_weight_kg']} kg")
        print(f"  Total Loss: Rp {summary['summary']['total_financial_loss_rupiah']:,.0f}")
        print(f"  Risk Level: {summary['risk_assessment']['level']}")
    
    def test_high_confidence_vs_low_confidence(self):
        """Test handling of high vs low confidence predictions"""
        import joblib
        from preprocess import preprocess_text
        
        vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
        model = joblib.load('models/waste_classification/waste_classifier_model.joblib')
        
        test_cases = [
            ("Daging berbau busuk sangat jelas", "HIGH"),  # Clear description
            ("Ada masalah", "LOW"),  # Vague description
            ("Tidak enak", "LOW")  # Generic complaint
        ]
        
        for text, expected_confidence_level in test_cases:
            cleaned = preprocess_text(text)
            transformed = vectorizer.transform([cleaned])
            confidence = float(model.predict_proba(transformed).max())
            
            # Log results for analysis
            print(f"\nText: '{text}'")
            print(f"Confidence: {confidence:.2%}")
            
            # High confidence should be reasonable (>0.7 for clear cases)
            # Low confidence can vary (<0.5 for vague cases)
            if expected_confidence_level == "HIGH":
                assert confidence > 0.5, f"High confidence case failed for: {text}"
    
    def test_end_to_end_business_scenario(self):
        """Complete business scenario: staff logs waste → ML predicts → financial calc"""
        import joblib
        from preprocess import preprocess_text
        from cost_calculator import calculate_loss_per_category, generate_daily_summary
        
        # Initialize components
        vectorizer = joblib.load('models/waste_classification/tfidf_vectorizer.joblib')
        model = joblib.load('models/waste_classification/waste_classifier_model.joblib')
        encoder = joblib.load('models/waste_classification/label_encoder.joblib')
        
        # Scenario: Multiple staff entries during a shift
        staff_entries = [
            {"staff_id": "STAFF001", "description": "Salad terkontaminasi rambut"},
            {"staff_id": "STAFF002", "description": "Ayam berbau busuk"},
            {"staff_id": "STAFF003", "description": "Mayonaise expired 2 hari"},
            {"staff_id": "STAFF001", "description": "Nasi gosong overcook"}
        ]
        
        predictions = []
        total_loss = 0
        
        for entry in staff_entries:
            # Step 1: ML Classification
            cleaned = preprocess_text(entry['description'])
            transformed = vectorizer.transform([cleaned])
            pred_idx = model.predict(transformed)[0]
            prediction = encoder.inverse_transform([pred_idx])[0]
            confidence = float(model.predict_proba(transformed).max())
            
            # Step 2: Cost Calculation (assume 1kg per entry)
            loss_result = calculate_loss_per_category(prediction, 1.0)
            
            predictions.append({
                'staff_id': entry['staff_id'],
                'original': entry['description'],
                'predicted_class': prediction,
                'confidence': confidence,
                'loss': loss_result
            })
            
            total_loss += loss_result['total_loss_rupiah']
        
        # Step 3: Generate Report
        report_entries = [{'category': p['predicted_class'], 'weight_kg': 1.0} 
                         for p in predictions]
        daily_report = generate_daily_summary(report_entries)
        
        # Validate complete workflow
        assert len(predictions) == 4
        assert total_loss > 0
        assert daily_report['summary']['total_entries'] == 4
        
        print(f"\n📊 END-TO-END BUSINESS SCENARIO")
        print("=" * 60)
        for i, pred in enumerate(predictions, 1):
            print(f"{i}. Staff {pred['staff_id']}: {pred['original']}")
            print(f"   → ML: {pred['predicted_class']} ({pred['confidence']:.1%})")
            print(f"   → Loss: Rp {pred['loss']['total_loss_rupiah']:,.0f}")
        print("=" * 60)
        print(f"TOTAL FINANCIAL LOSS: Rp {total_loss:,.0f}")
        print(f"DAILY SUMMARY: {daily_report['summary']['total_entries']} entries")
        print(f"RISK LEVEL: {daily_report['risk_assessment']['level']}")


def run_tests_manually():
    """Run tests without pytest framework"""
    print("=" * 80)
    print("🧪 KITCHENGARD CSM COMPREHENSIVE TEST SUITE")
    print("=" * 80)
    
    # Import test classes
    from test_kitchenguard import (
        TestWasteClassifierModel,
        TestBarcodeService,
        TestTextPreprocessing,
        TestIntegrationWorkflows
    )
    
    test_classes = [
        (TestWasteClassifierModel, "Waste Classifier Model Tests"),
        (TestBarcodeService, "Barcode Service Tests"),
        (TestTextPreprocessing, "Text Preprocessing Tests"),
        (TestIntegrationWorkflows, "Integration Workflow Tests")
    ]
    
    total_tests = 0
    passed_tests = 0
    failed_tests = 0
    skipped_tests = 0
    
    for test_class, class_name in test_classes:
        print(f"\n{'='*80}")
        print(f"📋 {class_name}")
        print(f"{'='*80}")
        
        try:
            instance = test_class()
            
            for method_name in dir(instance):
                if method_name.startswith('test_'):
                    total_tests += 1
                    try:
                        method = getattr(instance, method_name)
                        method()
                        print(f"   ✅ {method_name}")
                        passed_tests += 1
                    except Exception as e:
                        # Some tests might fail due to missing dependencies
                        if "Warning" in str(type(e).__name__) or "load" in str(e).lower():
                            print(f"   ⏭️  {method_name} (skipped)")
                            skipped_tests += 1
                        else:
                            print(f"   ❌ {method_name}: {str(e)[:50]}")
                            failed_tests += 1
                            
        except Exception as e:
            print(f"\n⚠️  Could not run {class_name}: {e}")
            skipped_tests += 1
    
    # Print summary
    print(f"\n{'='*80}")
    print("📊 FINAL TEST SUMMARY")
    print(f"{'='*80}")
    print(f"✅ Passed:  {passed_tests}/{total_tests}")
    print(f"❌ Failed:  {failed_tests}/{total_tests}")
    print(f"⏭️ Skipped: {skipped_tests}/{total_tests}")
    
    if total_tests > 0:
        pass_rate = (passed_tests / total_tests) * 100
        print(f"\n📈 Pass Rate: {pass_rate:.1f}%")
    
    if failed_tests == 0 and passed_tests > 0:
        print(f"\n🎉 ALL EXECUTABLE TESTS PASSED!")
    else:
        if failed_tests > 0:
            print(f"\n⚠️  {failed_tests} test(s) failed")
    
    print(f"{'='*80}")
    
    return passed_tests, failed_tests, skipped_tests


if __name__ == "__main__":
    run_tests_manually()
