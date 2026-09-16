"""
KitchenGuard CSM - Dataset Generator
Langkah 1 & 2: Pencarian, Pengumpulan, dan Pelabelan Dataset Teks Limbah & Kualitas Dapur
"""

import os
import random
import pandas as pd

# Seeds for reproducibility
random.seed(42)

# Template basis dan variasi kosakata dapur per kategori
CATEGORIES = {
    "SPOILED": {
        "subjects": [
            "tomat merah", "daging sapi tenderloin", "daging ayam fillet", "cabai rawit merah",
            "ikan kembung", "ikan kakap", "sayur bayam", "sayur kangkung", "wortel impor",
            "stroberi segar", "alpukat mentega", "telur ayam ras", "udang windu", "cumi-cumi segar",
            "daging cincang sapi", "brokoli hijau", "jamur tiram", "jamur kancing", "dada ayam fillet",
            "mentimun lokal", "paprika hijau", "paprika merah", "buncis sayur", "kol kubis putih",
            "daun selada romaine", "bawang merah kupas", "daging kambing", "ikan salmon fillet",
            "tahu putih sutra", "tempe bungkus", "keju cheddar buka kemasan", "daging bebek"
        ],
        "conditions": [
            "sudah berjamur putih dan kulit lembek berair",
            "berbau asam menyengat dan berubah warna keabu-abuan",
            "busuk basah dan mengeluarkan cairan lendir bau menyengat",
            "tekstur lembek berlendir dan timbul bintik hitam jamur",
            "insang pucat kehitaman mata cekung merah dan bau busuk",
            "layu menghitam membusuk dan berlendir di dalam chiller",
            "ditumbuhi kapang jamur abu-abu halus beracun",
            "bagian dalam busuk kehitaman berserat berlendir",
            "berbau belerang busuk dan mengeluarkan cairan keruh",
            "warna menggelap kecokelatan berlendir dan berbau busuk",
            "timbul lendir lengket dan aroma bangkai busuk",
            "membusuk di dasar kontainer penyimpanan dingin",
            "rasa asam tajam tidak wajar dan tekstur hancur berlendir",
            "hancur lembek mencair dan berbau busuk tidak layak konsumsi",
            "berlendir tebal bau tengik busuk menusuk hidung"
        ],
        "locations": [
            "di chiller 1", "di kulkas walk-in", "pada rak sayur bawah", "di kontainer prep",
            "pada wadah penyimpanan", "di laci pendingin", "di ruang penyimpanan basah",
            "saat dicek quality control pagi", "di rak seafood", "di cold storage dapur"
        ]
    },
    "EXPIRED": {
        "subjects": [
            "susu UHT full cream 1 liter", "saus tomat kemasan botol", "keju mozarella blok",
            "yogurt plain cup", "roti tawar gandum", "mayonnaise kemasan pouch 1 kg",
            "mentega tawar kaleng", "selai stroberi botol kaca", "santan kelapa instan",
            "ragi roti sachet", "sirup vanilla kemasan", "whipping cream cair 1 liter",
            "bumbu pasta kari kalengan", "sosis sapi bratwurst pack", "smoke beef slice kemasan",
            "keju parmesan bubuk jar", "saus tiram botol besar", "kecap manis kemasan refill",
            "tortilla wrap pack", "adonan puff pastry beku", "minyak wijen botol",
            "mie telur kering bungkus", "tepung custard kaleng", "krimer kental manis kaleng"
        ],
        "conditions": [
            "sudah lewat tanggal kadaluarsa 3 hari yang lalu",
            "expired date tercantum 12 September 2026 sudah terlampaui",
            "melewati batas tanggal best before pada label kemasan",
            "lewat masa simpan kadaluarsa 2 hari di kulkas pantry",
            "tanggal kedaluwarsa habis seminggu lalu belum terpakai",
            "sudah melewati masa kedaluwarsa dan kemasan agak menggelembung",
            "jatuh tempo tanggal expired pabrik kemarin sore",
            "tercatat lewat batas tanggal konsumsi aman di kemasan",
            "lewat masa kadaluarsa 5 hari tidak boleh dipakai masak",
            "stempel expired date menunjukkan tanggal minggu lalu",
            "melewati masa kadaluwarsa resmi dari distributor supplier",
            "batas aman konsumsi sudah lewat per hari ini",
            "tanggal kadaluarsa sudah habis saat pemeriksaan stok bulanan"
        ],
        "locations": [
            "di rak dry store", "pada chiller dairy", "di lemari stok bumbu",
            "di rak inventory pantry", "pada kulkas pastry", "di gudang bahan kering",
            "saat rotasi stok FIFO", "di rak saus dan kondimen", "pada area bakery"
        ]
    },
    "PREP_WASTE": {
        "subjects": [
            "kulit wortel dan bonggol brokoli", "lemak berlebih dan urat keras",
            "kulit bawang bombay dan akar seledri", "tulang dan kepala ikan kakap",
            "kulit nanas dan bagian hati yang keras", "batang kangkung tua dan daun layu",
            "kulit kentang kupasan", "cangkang telur ayam", "kulit dan ekor udang",
            "bonggol kubis dan daun luar yang keras", "ujung batang asparagus dan kulit tebal",
            "biji dan serat labu kuning", "tulang ayam karkas sisa deboning", "kulit jahe dan lengkuas",
            "tangkai cabai dan biji cabai merah", "ujung buncis dan serat samping",
            "kulit bawang putih dan bonggol akar", "potongan kulit semangka sisa garnish",
            "lemak brisket sisa pemotongan daging", "kulit dan biji mangga", "kulit apel kupasan"
        ],
        "conditions": [
            "sisa pembersihan dan pemotongan persiapan sup pagi",
            "hasil trimming kotor persiapan menu steak malam",
            "sisa kupasan persiapan bumbu dasar dapur utama",
            "sisa proses fillet untuk menu ikan bakar banquet",
            "bagian tidak dapat dikonsumsi dibuang saat prep siang",
            "hasil pemilahan bahan sebelum proses memasak",
            "sisa kupas 15 kg kentang untuk mashed potato",
            "sisa pemecahan telur pembuatan adonan kue bakery",
            "sisa kupasan prep menu udang goreng tepung",
            "bagian keras yang tidak dipakai pada resep utama",
            "sisa trimming fillet dada ayam untuk sate",
            "hasil pembersihan kulit dan akar sayuran",
            "sisa pemotongan rapi agar bentuk plating seragam"
        ],
        "locations": [
            "di meja butcher station", "pada station vegetable prep", "di area pastry preparation",
            "di meja butchery daging", "pada bak pembuangan prep station", "di fish station",
            "saat mise en place pagi", "pada talenan preparation"
        ]
    },
    "OVERCOOKED": {
        "subjects": [
            "daging steak sirloin", "saus bechamel keju", "potongan ayam goreng tepung",
            "nasi goreng seafood spesial", "sponge cake vanila", "bawang merah goreng",
            "sayur lodeh santan", "patty burger daging sapi", "karamel gula cair",
            "pasta fettuccine carbonara", "daging rendang sapi", "ikan nila goreng",
            "kue brownies panggang", "sup iga sapi", "telur dadar telur gulung",
            "crust pizza mozarella", "tahu goreng krispi", "roti bakar srikaya"
        ],
        "conditions": [
            "gosong hitam legam karena api grill terlalu besar",
            "hangus berkerak di dasar panci dan timbul bau sangit",
            "terlalu lama digoreng di deep fryer sampai kering mengeras pahit",
            "gosong terbakar dan berkerak pahit saat jam sibuk dinner",
            "bantat dan tepi loyang hangus terbakar di oven bersuhu tinggi",
            "terlalu lama di wajan penggorengan hingga gosong pahit",
            "terlalu lama direbus tekstur hancur lebur kuah keruh gosong",
            "gosong di luar tetapi bagian dalam masih belum matang",
            "hangus hitam berasap pekat dan bau gosong tajam",
            "kelewat matang lembek hancur menjadi bubur",
            "gosong terbakar api wajan karena ditinggal memasak",
            "terbakar kering dan terasa pahit tidak bisa disajikan ke tamu",
            "kelebihan waktu masak di salamander hingga keju hangus pahit"
        ],
        "locations": [
            "di hot line station", "pada kompor saute", "di area deep fryer",
            "pada oven baking", "di grill station", "saat proses plating order",
            "di wajan stir-fry wok", "pada panci stock pot"
        ]
    },
    "CONTAMINATED": {
        "subjects": [
            "potongan daging wagyu MB7", "daun selada hijau segar", "saus salad dressing thousand island",
            "adonan sup asparagus kepiting", "ikan salmon fillet segar", "minyak goreng baru di penggorengan",
            "keju mozarella parut", "tepung terigu serbaguna 5 kg", "potongan buah melon salad",
            "dada ayam marinasi", "nasi timbel matang", "es batu kristal minuman",
            "bumbu kacang sate", "potongan roti burger bun", "saus sambal mangkok saji"
        ],
        "conditions": [
            "jatuh ke lantai dapur yang kotor dan berminyak",
            "terkena cipratan cairan pembersih sabun pel lantai",
            "kemasukan serangga lalat yang terbang di area meja plating",
            "terkontaminasi pecahan beling piring kaca yang pecah di dekatnya",
            "bersentuhan langsung dengan pisau dan talenan kotor belum dicuci",
            "kemasukan tetesan air kotor bocoran dari plafon exhaust dapur",
            "tersentuh tangan staff tanpa sarung tangan yang sedang flu batuk",
            "terkontaminasi kotoran hama tikus di rak penyimpanan bawah",
            "tercampur serpihan plastik wrap kemasan tajam yang sobek",
            "tertetes cairan darah ayam mentah dari rak chiller bagian atas",
            "kemasukan rambut staff dapur saat proses plating makanan",
            "terkena debu kotor tebal akibat renovasi ventilasi dapur",
            "tercampur bahan alergen kacang yang dilarang pada pesanan khusus"
        ],
        "locations": [
            "di area plating pass", "pada meja prep station", "di lantai kitchen line",
            "pada rak chiller bawah", "di meja saji banquet", "di dekat tempat cuci piring pot wash",
            "pada area penyimpanan kering", "di pick-up table waiter"
        ]
    },
    "SURPLUS": {
        "subjects": [
            "nasi putih dan aneka lauk pauk", "sup buntut kuah rempah 15 porsi",
            "roti croissant dan pastry display", "salad sayur bar lengkap saus",
            "adonan saus kari jepang 5 liter", "nasi kuning tumpeng komplit",
            "daging ayam panggang bumbu rosemary", "pudding cokelat dessert 20 cup",
            "jus jeruk segar dispenser 10 liter", "pasta marinara saus tomat",
            "sate ayam madura 50 tusuk", "ikan bakar rica-rica 10 porsi",
            "bihun goreng spesial banquet", "kentang wedges panggang",
            "bubur ayam sarapan buffet", "soto ayam madura porsi besar"
        ],
        "conditions": [
            "sisa prasmanan buffet dinner hotel tidak habis dikonsumsi tamu",
            "kelebihan porsi karena acara pesta banquet batal sebagian",
            "sisa display etalase kafe malam hari yang tidak laku terjual",
            "sisa penutupan operasional restoran malam tidak boleh disimpan",
            "overproduksi sisa persiapan siang yang tidak habis malam ini",
            "kelebihan porsi pesanan katering luar yang sudah selesai",
            "sisa menu promo lunch siang yang berlebih dan tidak terjual",
            "sisa acara gathering tamu hotel yang tidak terkonsumsi",
            "sisa minuman sarapan pagi yang melebihi batas waktu saji",
            "stok porsi matang berlebih karena prediksi tamu meleset",
            "kelebihan kuota masak harian dan tidak dapat dibekukan kembali",
            "sisa sajian meeting room korporat yang selesai lebih awal"
        ],
        "locations": [
            "di buffet counter restaurant", "pada meja banquet hall", "di display showcase pastry",
            "di holding cabinet pemanas", "pada dispenser buffet sarapan", "di area room service",
            "di chiller holding akhir shift", "pada meja katering ballroom"
        ]
    }
}

# Tambahan variasi kata keterangan, penghubung, dan format catatan staff
CONNECTORS = [
    "", "ditemukan bahwa", "laporan staff:", "kondisi aktual:", "catatan reject:",
    "keterangan waste:", "insiden dapur:", "alasan pembuangan:"
]

ACTION_NOTES = [
    "", "harus segera dibuang ke tempat sampah organik.", "wajib di-write off dari sistem inventori.",
    "tidak layak disajikan ke customer.", "segera dibuatkan berita acara limbah.",
    "masuk log waste shift ini.", "direject oleh head chef saat inspeksi.",
    "dibuang sesuai SOP pencegahan bahaya makanan.", "dibatalkan untuk disajikan."
]

def generate_samples(target_per_class=150):
    rows = []
    
    for category, content in CATEGORIES.items():
        subjects = content["subjects"]
        conditions = content["conditions"]
        locations = content["locations"]
        
        # Buat kombinasi alami
        generated_set = set()
        count = 0
        
        # 1. Pola standar Subjek + Kondisi + Lokasi
        for subj in subjects:
            for cond in conditions:
                for loc in locations:
                    text = f"{subj} {cond} {loc}."
                    if text not in generated_set:
                        generated_set.add(text)
                        rows.append({"text": text, "category": category})
                        count += 1
                        if count >= target_per_class:
                            break
                if count >= target_per_class:
                    break
            if count >= target_per_class:
                break
                
        # 2. Variasi kalimat pendek & kalimat kasual (gaya catatan cepat chef)
        casual_patterns = [
            lambda s, c: f"{s} {c}",
            lambda s, c: f"Reject {s}, {c}",
            lambda s, c: f"Waste log: {s} {c}",
            lambda s, c: f"{s} {c}, buang segera",
            lambda s, c: f"Kondisi {s}: {c}"
        ]
        
        for i in range(30):
            s = random.choice(subjects)
            c = random.choice(conditions)
            pattern = random.choice(casual_patterns)
            text = pattern(s, c)
            if text not in generated_set:
                generated_set.add(text)
                rows.append({"text": text, "category": category})
                
    df = pd.DataFrame(rows)
    # Acak urutan baris
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)
    return df

if __name__ == "__main__":
    output_path = os.path.join("data", "kitchenguard_waste_dataset.csv")
    df = generate_samples(target_per_class=150)
    df.to_csv(output_path, index=False, encoding="utf-8")
    print(f"Dataset berhasil dibuat: {output_path}")
    print(f"Total baris: {len(df)}")
    print("\nDistribusi Kelas:")
    print(df["category"].value_counts())
