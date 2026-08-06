# Dynamic S-Box berbasis Kurva Eliptik untuk AES-128

Implementasi Java pembangkitan **dynamic S-box** berbasis kurva eliptik, membandingkan empat rancangan **official** (Bab 4): baseline statis, baseline EC shuffle-only, **CBPE**, dan **KDFP**.

Juga menyertakan arsip rancangan **eksplorasi pra-final E1–E8** (Lampiran A) di folder `exploratory/` — **bukan** klaim hasil final.

**Author:** Nathan Dava Arkananta (1301223297) — S1 Informatika, Universitas Telkom  
**Stack:** Java + Bouncy Castle (`bcprov-jdk18on-1.83.jar`)

**Judul TA (disetujui dospem):**  
*Pengembangan S-Box Dinamis Berbasis Kurva Eliptik untuk AES-128 Menggunakan Pembangkit Bilangan Acak dan Parameter Kunci*

## Naskah Tugas Akhir (PDF)

| Berkas | Keterangan |
|:---|:---|
| [`docs/TugasAkhir_1301223297_NathanDavaArkananta.pdf`](docs/TugasAkhir_1301223297_NathanDavaArkananta.pdf) | Ekspor PDF naskah revisi pasca sidang (judul dospem + Lampiran A/B) |

## Official (Bab 4) — folder `src/`

| Berkas | Varian | Deskripsi |
|:---|:---|:---|
| `src/Varian1_StaticAES.java` | V1 | S-box statis AES-128 (acuan) |
| `src/Varian2_StandardizedBaseline.java` | V2 | Baseline EC shuffle-only |
| `src/Varian3_CBPE.java` | V3 | CBPE — affine-equivalent, penekanan parameter input |
| `src/Varian4_KDFP.java` | V4 | KDFP — affine-equivalent, penekanan parameter output |

Parameter eksperimen: kurva **secp256r1**, **L = 4096**, V3/V4 **32 kandidat D** (NL-first), **100 run** per varian.

### Kompilasi & jalankan (official)

```bat
cd src
javac -encoding UTF-8 -cp ..\lib\bcprov-jdk18on-1.83.jar Varian1_StaticAES.java Varian2_StandardizedBaseline.java Varian3_CBPE.java Varian4_KDFP.java
java -cp ..\lib\bcprov-jdk18on-1.83.jar;. Varian3_CBPE
```

### Hasil official

Folder `results/`:

- `bab4_benchmark_100runs.md` / `.csv`
- `benchmark_varian*_rev2.log`

| Varian | NL | DU | SAC |
|:---:|:---:|:---:|:---:|
| V1 | 112,0 | 4,0 | 0,5047 |
| V2 | 92,8 | 11,2 | 0,5028 |
| V3 (CBPE) | 112,0 | 4,0 | 0,5000 |
| V4 (KDFP) | 112,0 | 4,0 | 0,5000 |

## Lampiran naskah (mapping)

| Lampiran buku | Isi |
|:---|:---|
| **A** | Repositori ini (kode sumber) — URL halaman GitHub |
| **B** | Tabel metrik evaluasi E1–E8 (pra-final), bukan klaim Bab 4 |

## Exploratory E1–E8 (pra-final) — folder `exploratory/`

Rancangan trial-error **sebelum** desain final affine. Nama file **ber-prefix E1–E8** (tanpa nama `Varian3_*` / `Varian4_*`). Sumber kode untuk **Lampiran A**; angka metrik di naskah ada di **Lampiran B**.

Lihat **[`exploratory/README.md`](exploratory/README.md)** untuk mapping lengkap.

| Label | File |
|:---:|:---|
| E1 | `exploratory/E1_Fusion_MaterialMix.java` |
| E2 | `exploratory/E2_Stage1_KeyedByteMixing.java` |
| E3 | `exploratory/E3_RNG_CoordinateExtract.java` |
| E4 | `exploratory/E4_LightPerturbation.java` |
| E5 | `exploratory/E5_InterleaveMaterial.java` |
| E6 | `exploratory/E6_FinalPermute_RNG.java` |
| E7 | `exploratory/E7_FinalPermute_Revised.java` |
| E8 | `exploratory/E8_HybridPermute_RNG.java` |

## Struktur folder

```text
hybrid-ec-dynamic-sbox-aes128/
  README.md
  docs/
  lib/bcprov-jdk18on-1.83.jar
  src/                 # official V1–V4
  exploratory/         # E1–E8 pra-final (Lampiran A)
  results/             # benchmark official 100-run
```
