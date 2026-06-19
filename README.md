# Hybrid EC Dynamic S-Box for AES-128

Implementasi Java pembangkitan **dynamic S-box** untuk AES-128 berbasis kurva eliptik, membandingkan empat varian: baseline statis, baseline EC shuffle-only, **CBPE**, dan **KDFP**.

**Author:** Nathan Dava Arkananta (1301223297) — S1 Informatika, Universitas Telkom  
**Stack:** Java + Bouncy Castle (`bcprov-jdk18on-1.83.jar`)

## Naskah Tugas Akhir (PDF)

| Berkas | Keterangan |
|:---|:---|
| [`docs/TugasAkhir_1301223297_NathanDavaArkananta.pdf`](docs/TugasAkhir_1301223297_NathanDavaArkananta.pdf) | Naskah lengkap Buku Tugas Akhir (ekspor dari `18JULI_Tugas Akhir - Nathan Dava Arkananta.docx`) |

## Berkas Sumber Utama

| Berkas | Varian | Deskripsi |
|:---|:---|:---|
| `Varian1_StaticAES.java` | V1 | S-box statis AES-128 |
| `Varian2_StandardizedBaseline.java` | V2 | Baseline EC shuffle-only (8 kandidat D, seleksi entropi) |
| `Varian3_CBPE.java` | V3 | CBPE — affine-equivalent, penekanan parameter input dari h0 |
| `Varian4_KDFP.java` | V4 | KDFP — affine-equivalent, penekanan parameter output dari h1 |

Parameter eksperimen (BAB 4): kurva **secp256r1**, **L = 4096**, V3/V4 **32 kandidat D** dengan seleksi **NL-first**, **100 run** per varian.

## Kompilasi

Dari folder `src/`:

```bat
javac -encoding UTF-8 -cp ..\lib\bcprov-jdk18on-1.83.jar Varian1_StaticAES.java Varian2_StandardizedBaseline.java Varian3_CBPE.java Varian4_KDFP.java
```

## Menjalankan

```bat
java -cp ..\lib\bcprov-jdk18on-1.83.jar;. Varian1_StaticAES
java -cp ..\lib\bcprov-jdk18on-1.83.jar;. Varian2_StandardizedBaseline
java -cp ..\lib\bcprov-jdk18on-1.83.jar;. Varian3_CBPE
java -cp ..\lib\bcprov-jdk18on-1.83.jar;. Varian4_KDFP
```

## Hasil Pengujian (sudah dijalankan)

Folder `results/` berisi log dan ringkasan benchmark 100 run:

- `bab4_benchmark_100runs.md` — tabel ringkasan
- `bab4_benchmark_100runs.csv` — data tabular
- `benchmark_varian*_rev2.log` — log mentah per varian

Nilai rata-rata (selaras Tabel 4.2 naskah):

| Varian | NL | DU | SAC |
|:---:|:---:|:---:|:---:|
| V1 | 112,0 | 4,0 | 0,5047 |
| V2 | 92,8 | 11,2 | 0,5028 |
| V3 (CBPE) | 112,0 | 4,0 | 0,5000 |
| V4 (KDFP) | 112,0 | 4,0 | 0,5000 |

## Struktur Folder

```
hybrid-ec-dynamic-sbox-aes128/
  README.md
  docs/TugasAkhir_1301223297_NathanDavaArkananta.pdf
  lib/bcprov-jdk18on-1.83.jar
  src/Varian1_StaticAES.java
  src/Varian2_StandardizedBaseline.java
  src/Varian3_CBPE.java
  src/Varian4_KDFP.java
  results/bab4_benchmark_100runs.md
  results/bab4_benchmark_100runs.csv
  results/benchmark_varian*_rev2.log
```
