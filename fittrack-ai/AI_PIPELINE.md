# FITTRACK AI — Multimodal AI Pipeline Specification

## 1. Vision Recognition Pipeline

The core AI engine receives captured plate photographs from the Android client and performs multi-item segmentation, candidate matching, and portion estimation.

```
[Camera / Gallery JPG] 
         │
         ▼
[Base64 Encoding / Buffer] 
         │
         ▼
[Gemini 1.5 Flash Multimodal Vision] 
         │ (Structured JSON Prompt)
         ▼
[Raw Detection: Items, Portions, Cooking Method] 
         │
         ▼
[Nutrition Matcher: USDA FDC & IFCT 2017] 
         │
         ▼
[Mathematical Portion Scaling & Oil Multipliers] 
         │
         ▼
[Validated Detection JSON + Confidence Metrics] 
         │
         ▼
[Android Client: Interactive Portion Adjustment & User Review]
```

---

## 2. Gemini Multimodal Adapter (`GeminiVisionAdapter`)

### System Prompt & Schema
The Gemini 1.5 Flash model is instructed with a strict JSON system prompt:
- Identify every distinct food component on the plate (e.g. *Rice*, *Sambar*, *Potato Fry*, *Curd*).
- Estimate gram weight based on visual vessel proportions and depth cues.
- Infer cooking method (*raw*, *boiled*, *steamed*, *shallow_fried*, *deep_fried*).
- Infer oil level (*low*, *normal*, *high*).
- Assign an item detection confidence score between `0.0` and `1.0`.

### Provider-Agnostic Interface
The system implements `BaseVisionAdapter`:
```python
class BaseVisionAdapter(ABC):
    @abstractmethod
    async def analyze_food_image(
        self,
        image_bytes: bytes,
        user_hint: Optional[str] = None
    ) -> RawScanResult:
        pass
```
- **Live Mode**: Calls Gemini API with structured output schema.
- **Mock Mode**: Heuristic matching for automated testing and offline environments.

---

## 3. Speech-to-Meal Parser (`VoiceMealParser`)

Processes spoken sentences into itemized nutrient entities:
- **Number Parsing**: Converts textual numbers (`"two"`, `"three"`, `"half"`) to floats.
- **Unit Normalization**: Resolves `"grams"`, `"g"`, `"dosas"`, `"cups"`, `"bowls"`, `"teaspoons"`.
- **Language Support**: Handles English, Indian English, and Tanglish mixed terms (*"two dosas with sambar"*, *"200 grams chicken biryani"*, *"four teaspoons of ghee"*).

---

## 4. Nutrition Facts OCR Parser (`LabelOcrParser`)

Extracts structured data from packaged food labels:
- Matches regex patterns for Calories, Protein, Total Fat, Carbohydrates, Fiber, Sugar, and Sodium.
- Validates whether numbers are Per 100g or Per Serving.
- Prevents inventing missing micronutrients.
