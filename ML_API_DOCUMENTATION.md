# KitchenGuard CSM - Machine Learning REST API Documentation

## Overview
FastAPI-based REST API untuk inferensi model Machine Learning Waste Classification dengan preprocessing yang konsisten dan validasi versi.

## Server Configuration

### Port
- Default: `8000`
- Host: `0.0.0.0` (accessible from external)

### Starting the Server
```bash
python run_api.py
```

The server will automatically:
1. Set correct working directory
2. Load ML models from `models/waste_classification/`
3. Validate preprocessing version compatibility
4. Start FastAPI with CORS enabled

## Endpoints

### 1. Health Check

**Endpoint:** `GET /health`

**Description:** Returns API and model status information

**Response:**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "preprocessing_version": "2.0.0",
  "model_loaded": true,
  "timestamp": "2026-09-23T16:21:35.276340"
}
```

**Example Request:**
```bash
curl http://localhost:8000/health
```

---

### 2. Waste Prediction

**Endpoint:** `POST /api/ml/waste/predict`

**Description:** Predict waste category from text description using trained ML model

**Request Body:**
```json
{
  "description": "string - Description of waste in Indonesian"
}
```

**Response:**
```json
{
  "label": "SPOILED",
  "confidence": 1.0,
  "model_version": "2.0.0",
  "preprocessing_version": "2.0.0",
  "timestamp": "2026-09-23T16:21:48.404137"
}
```

**Available Labels:**
- `CONTAMINATED` - Critical contamination detected
- `EXPIRED` - Expired products
- `OVERCOOKED` - Overcooked food items
- `PREP_WASTE` - Preparation waste (trimming, peeling)
- `SPOILED` - Spoiled/moldy ingredients
- `SURPLUS` - Surplus/unsold food

**Example Requests:**

1. **Spoiled Detection:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "tomat merah berjamur putih dan kulit lembek berlendir di chiller"}'
```

2. **Expired Detection:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "saus tomat kemasan botol expired date sudah terlampaui"}'
```

3. **Contamination Detection:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "daun selada jatuh ke lantai dapur yang kotor"}'
```

4. **Overcooking Detection:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "daging steak terlalu lama digoreng sampai kering mengeras"}'
```

5. **Prep Waste:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "kulit wortel dan bonggol brokoli sisa trimming"}'
```

6. **Surplus Food:**
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "nasi putih sisa menu promo lunch siang tidak terjual"}'
```

---

### 3. Model Information

**Endpoint:** `GET /api/ml/models`

**Description:** Returns detailed information about loaded models

**Response:**
```json
{
  "model_type": "MultinomialNB",
  "n_classes": 6,
  "classes": ["CONTAMINATED", "EXPIRED", "OVERCOOKED", "PREP_WASTE", "SPOILED", "SURPLUS"],
  "vectorizer_vocab_size": 1205,
  "preprocessing_version": "2.0.0",
  "model_version": "2.0.0",
  "timestamp": "2026-09-23T16:21:35.276340"
}
```

---

## Technical Details

### Preprocessing Consistency
- Uses same Sastrwi stemming and stopword removal as training
- Version control ensures train/inference consistency
- Preprocessing version v2.0.0 verified on startup

### Model Loading
Models loaded from:
- `/models/waste_classification/waste_classifier_model.joblib`
- `/models/waste_classification/tfidf_vectorizer.joblib`
- `/models/waste_classification/label_encoder.joblib`

### Error Handling

**Empty Input:**
```json
{"detail":"Description field is required and cannot be empty"}
```

**Missing Field:**
```json
{"detail":[{"type":"missing","loc":["body","description"],"msg":"Field required","input":{}}]}
```

**Validation Errors:**
- Pydantic automatically validates request schema
- HTTP 400 for validation errors
- HTTP 503 if models not loaded

### CORS Configuration
- Enabled for all origins (`*`)
- Credentials allowed
- All methods and headers permitted
- Configured for Web Admin integration

## Integration Examples

### JavaScript/Fetch API (Web Admin)
```javascript
async function predictWaste(description) {
  const response = await fetch('http://localhost:8000/api/ml/waste/predict', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ description })
  });
  
  const result = await response.json();
  console.log(`Category: ${result.label}, Confidence: ${result.confidence}`);
  
  return result;
}

// Usage
predictWaste("tomat merah berjamur").then(console.log);
```

### Python Requests
```python
import requests

def predict_waste(description):
    response = requests.post(
        'http://localhost:8000/api/ml/waste/predict',
        json={"description": description}
    )
    return response.json()

# Usage
result = predict_waste("saus tomat expired")
print(f"Label: {result['label']}")
print(f"Confidence: {result['confidence']}")
```

### cURL Command
```bash
curl -X POST http://localhost:8000/api/ml/waste/predict \
  -H "Content-Type: application/json" \
  -d '{"description": "your waste description here"}'
```

## Testing

### Run All Tests
```bash
python app/api/test_api.py
```

This will test:
1. Health endpoint
2. All 6 prediction categories
3. Error handling
4. Model info endpoint

### Individual Endpoint Tests
See examples above or run `app/api/test_api.py`

## Performance Notes

- Average inference time: ~5-10ms per prediction
- Memory usage: ~50MB for loaded models
- Cold start: ~3 seconds for model loading
- Hot start: <100ms (models already loaded)

## Security Considerations

1. **Production Deployment:**
   - Replace CORS `allow_origins=["*"]` with specific domains
   - Add authentication middleware
   - Enable rate limiting
   - Use HTTPS instead of HTTP

2. **Model Protection:**
   - Models stored in non-public directory
   - Access controlled via API only

3. **Input Validation:**
   - Text length limits can be added
   - SQL injection prevention (not applicable - no database)
   - Buffer overflow protection (handled by Python)

## Troubleshooting

### Issue: "ModuleNotFoundError: No module named 'preprocess'"
**Solution:** Ensure you're running via `run_api.py` which sets correct paths

### Issue: "Could not find model files"
**Solution:** 
1. Check models exist in `models/waste_classification/`
2. Verify file permissions
3. Confirm working directory is project root

### Issue: "Preprocessing version mismatch"
**Solution:** 
1. Retrain models with current preprocess version
2. Or downgrade preprocessing to match model

### Issue: CORS blocked by browser
**Solution:**
1. Check server is running
2. Verify Origin header matches CORS allowlist
3. Check browser console for specific error messages

## Version History

- **v1.0.0** (2026-09-23): Initial release with:
  - FastAPI REST interface
  - Consistent preprocessing
  - 6 waste classification categories
  - CORS support
  - Health check endpoint
  - Error handling

## Support

For issues or questions:
- Check logs in terminal where server is running
- Review `ML_API_DOCUMENTATION.md`
- Examine `test_api.py` for examples
