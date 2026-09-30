"""
KitchenGuard CSM - Text Preprocessing Module
Langkah 3: Pembersihan & Praproses Teks
Menghapus duplikasi, data kosong, tanda tidak perlu; case folding, tokenisasi, stopword removal, stemming.
"""

import re
import string
import pandas as pd
from Sastrawi.StopWordRemover.StopWordRemoverFactory import StopWordRemoverFactory
from Sastrawi.Stemmer.StemmerFactory import StemmerFactory

# Inisialisasi Factory Sastrawi
_stopword_factory = StopWordRemoverFactory()
_base_stopwords = set(_stopword_factory.get_stop_words())

# Stopwords operasional tambahan (kata hubung/keterangan yang tidak memiliki bobot sentimen/kategori)
_custom_stopwords = {
    "yg", "yang", "dan", "di", "ke", "dari", "pada", "oleh", "untuk", "dengan", "ini", "itu",
    "saat", "lalu", "sudah", "telah", "sedang", "bisa", "dapat", "ada", "karena", "tersebut",
    "segera", "wajib", "harus", "kemarin", "hari", "per", "pada", "saat", "bagian", "hasil"
}
ALL_STOPWORDS = _base_stopwords.union(_custom_stopwords)

_stemmer_factory = StemmerFactory()
STEMMER = _stemmer_factory.create_stemmer()

# In-memory dictionary cache untuk mempercepat stemming kata berulang
_STEM_CACHE = {}

def get_stemmed_word(word: str) -> str:
    """Mengambil hasil stem dari cache atau memprosesnya menggunakan Sastrawi."""
    if word not in _STEM_CACHE:
        _STEM_CACHE[word] = STEMMER.stem(word)
    return _STEM_CACHE[word]

def normalize_spaced_characters(text: str) -> str:
    """
    Sub-langkah robustness: Menangani input teks ber-spasi antar huruf
    (misal: 'A y a m   g o s o n g' atau 'd a g i n g') agar kembali menjadi kata utuh.
    """
    if not isinstance(text, str) or not text.strip():
        return ""
    # Gabungkan huruf-huruf tunggal yang dipisahkan 1 spasi: 'A y a m' -> 'Ayam'
    pattern = r'\b[a-zA-Z](?: [a-zA-Z])+\b'
    def merge_single_letters(m):
        return ''.join(m.group(0).split(' '))
    normalized = re.sub(pattern, merge_single_letters, text)
    
    # Tangani kasus kata majemuk yang tergabung tanpa spasi dari variasi noise (misal 'gosonghangus')
    compound_splits = [
        ("gosonghangus", "gosong hangus"),
        ("berbaubusuk", "berbau busuk"),
        ("busuklendir", "busuk lendir"),
        ("jatuhkelantai", "jatuh ke lantai"),
    ]
    for combined, separated in compound_splits:
        normalized = re.sub(r'\b' + combined + r'\b', separated, normalized, flags=re.IGNORECASE)
        
    return normalized

def clean_and_case_fold(text: str) -> str:
    """
    Sub-langkah 3a: Case Folding & Pembersihan Karakter
    - Ubah huruf menjadi huruf kecil (lowercase).
    - Normalisasi input ber-spasi antar huruf (robustness).
    - Hapus URL, tanda baca, angka yang berdiri sendiri, dan whitespace berlebih.
    """
    if not isinstance(text, str):
        return ""
    
    # Case folding
    text = text.lower()
    
    # Normalisasi teks ber-spasi antar huruf
    text = normalize_spaced_characters(text)
    
    # Hapus URL jika ada
    text = re.sub(r"https?://\S+|www\.\S+", " ", text)
    
    # Hapus karakter spesial dan tanda baca
    text = re.sub(r"[%s]" % re.escape(string.punctuation), " ", text)
    
    # Hapus angka yang berdiri sendiri
    text = re.sub(r"\b\d+\b", " ", text)
    
    # Normalisasi spasi berulang
    text = re.sub(r"\s+", " ", text).strip()
    return text

def tokenize(text: str) -> list[str]:
    """
    Sub-langkah 3b: Tokenisasi
    Memisahkan string kalimat menjadi list kata/token.
    """
    if not text:
        return []
    return text.split()

def remove_stopwords(tokens: list[str]) -> list[str]:
    """
    Sub-langkah 3c: Stopword Removal
    Menyaring token dari kata umum yang tidak membawa informasi kategori limbah.
    """
    return [token for token in tokens if token not in ALL_STOPWORDS and len(token) > 1]

def stem_tokens(tokens: list[str]) -> list[str]:
    """
    Sub-langkah 3d: Stemming
    Mengembalikan token kata ke bentuk dasarnya (root word).
    """
    return [get_stemmed_word(token) for token in tokens]

def preprocess_text(text: str) -> str:
    """
    Pipeline lengkap praproses satu baris teks:
    Teks Mentah -> Clean & Lowercase -> Tokenisasi -> Stopword Removal -> Stemming -> Teks Bersih
    """
    cleaned = clean_and_case_fold(text)
    tokens = tokenize(cleaned)
    filtered = remove_stopwords(tokens)
    stemmed = stem_tokens(filtered)
    return " ".join(stemmed)

def clean_and_preprocess_dataframe(df: pd.DataFrame, text_col: str = "text", target_col: str = "category") -> pd.DataFrame:
    """
    Pembersihan menyeluruh pada DataFrame:
    1. Hapus baris dengan nilai null/kosong pada kolom teks atau target.
    2. Hapus duplikasi teks identik.
    3. Lakukan praproses teks pada setiap baris.
    4. Hapus baris yang hasil praprosesnya menjadi kosong.
    """
    initial_count = len(df)
    
    # 1. Hapus data kosong
    df = df.dropna(subset=[text_col, target_col]).copy()
    
    # 2. Hapus duplikasi
    df = df.drop_duplicates(subset=[text_col]).copy()
    
    print(f"[Prapemrosesan] Data awal: {initial_count} baris -> Setelah hapus duplikat/null: {len(df)} baris")
    
    # 3. Lakukan praproses teks
    print("[Prapemrosesan] Menjalankan case folding, tokenisasi, stopword removal, dan stemming...")
    df["clean_text"] = df[text_col].apply(preprocess_text)
    
    # 4. Hapus teks hasil praproses yang kosong
    df = df[df["clean_text"].str.strip() != ""].copy()
    
    print(f"[Prapemrosesan] Selesai! Data siap representasi: {len(df)} baris.")
    return df

if __name__ == "__main__":
    sample = "Tomat merah sudah berjamur putih dan kulitnya lembek berair di chiller 1!"
    print(f"Teks Asli      : {sample}")
    print(f"Hasil Praproses: {preprocess_text(sample)}")

# =====================
# OOV (Out of Vocabulary) Keyword Matching Fallback
# =====================

def oov_keyword_fallback(text: str) -> dict:
    """
    Fallback mechanism untuk teks yang OUT OF VOCABULARY (tidak ada di training data).
    Menggunakan keyword-based classification sebagai safety net.
    
    Returns: {
        "category": str,  # Predicted waste category
        "confidence": float,  # Confidence score (0-1)
        "matched_keywords": list[str],  # Keywords that triggered this prediction
        "fallback_used": True,
    }
    """
    
    text_lower = text.lower()
    
    # Keyword patterns per category
    keyword_patterns = {
        "CONTAMINATED": [
            "terkontaminasi", "contaminated", "lantai", "hair", "lunas", 
            "sewage", "pest", "rodent", "tikus", "serangga", "shard", "kaca",
            "logam", "metal", "chemical", "pembersih", "tangan", "bersentuh",
            "berkontak", "jatuh", "floor", "bakteri", "virus"
        ],
        "SPOILED": [
            "berjamur", "moldy", "jamur", "busuk", "rotten", "bau", "smell",
            "lendir", "slimy", "lembek", "mushy", "kebiruan", "kehijauan",
            "discoloration", "curdled", "sour", "asam", "raneid", "busuk",
            "off smell", "mould", "fermentasi", "tapai", "basi", "apek", "anyir"
        ],
        "EXPIRED": [
            "expired", "kedaluwarsa", "mhd", "kadaluwarsa", "lewat", "melewati",
            "expiry", "date expired", "sudah expiry", "telah expired", "expired date",
            "bebeku", "thawed", "melted", "dicairkan", "frozen", "cair",
            "melting", "refreeze", "bekuan"
        ],
        "OVERCOOKED": [
            "gosong", "burnt", "hangus", "overcook", "terbakar", "hitam",
            "charred", "scorched", "kehitaman", "coklat tua", "burning",
            "overdone", "keras", "kering", "hard", "dry", "pengap"
        ],
        "PREP_WASTE": [
            "trimming", "trim", "peeling", "potongan", "sisa", "prep",
            "cutting board", "skinning", "boning", "diced", "chopped",
            "memotong", "mengupas", "menghilangkan", "kulit", "tulang", "urat"
        ],
        "SURPLUS": [
            "surplus", "excess", "leftover", "unserved", "tidak terjual",
            "lebih", "sisa pelayanan", "remaining", "extra", "unplanned",
            "tidak tersentuh", "dimakan customer", "plate scrapings"
        ]
    }
    
    # Score each category based on keyword matches
    scores = {}
    matched_keywords = {}
    
    for category, keywords in keyword_patterns.items():
        matches = sum(1 for kw in keywords if kw in text_lower)
        scores[category] = matches
        matched_keywords[category] = [kw for kw in keywords if kw in text_lower]
    
    # Find best match
    best_category = max(scores, key=scores.get)
    best_score = scores[best_category]
    
    # Calculate confidence based on number of matched keywords
    confidence = min(best_score / max(len(keyword_patterns[best_category]) * 0.3, 1), 0.95)
    
    # Minimum confidence floor
    if best_score > 0 and confidence < 0.3:
        confidence = 0.3 + (best_score * 0.1)
    
    return {
        "category": best_category,
        "confidence": round(confidence, 3),
        "matched_keywords": matched_keywords[best_category],
        "fallback_used": True,
        "score_breakdown": scores
    }
