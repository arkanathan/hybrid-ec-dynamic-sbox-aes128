# Hasil Benchmark — 100 Run per Varian

**Tanggal:** 18 Juni 2026  
**Lingkungan:** Java + Bouncy Castle (`bcprov-jdk18on-1.83.jar`)  
**Kurva:** secp256r1 | **L:** 4096  
**Seleksi D:** Varian 2 = 8 kandidat (entropi); Varian 3 & 4 = 32 kandidat (NL-first)  
**Metrik NL:** vectorial nonlinearity (255 mask bit keluaran)

## Tabel Ringkasan

| Varian | Keterangan | NL (rata-rata) | SAC (rata-rata) | DU (rata-rata) | Waktu (ms) | Memori (KB) |
|:------:|------------|:--------------:|:---------------:|:--------------:|:----------:|:-----------:|
| 1 | S-box statis AES-128 | 112,0 | 0,5047 | 4,0 | 0,85 | 18.689 |
| 2 | Baseline EC shuffle-only | 92,8 | 0,5028 | 11,2 | 687,31 | 66.327 |
| 3 | CBPE | 112,0 | 0,5000 | 4,0 | 699,70 | 33.190 |
| 4 | KDFP | 112,0 | 0,5000 | 4,0 | 738,20 | 47.404 |

## Selisih terhadap Varian 2

| Varian | ΔNL | ΔDU | ΔWaktu (ms) |
|:------:|:---:|:---:|:-----------:|
| 3 | +19,2 | −7,2 | +12 |
| 4 | +19,2 | −7,2 | +51 |

## Log Mentah

- `benchmark_varian1_rev2.log`
- `benchmark_varian2_rev2.log`
- `benchmark_varian3_rev2.log`
- `benchmark_varian4_rev2.log`