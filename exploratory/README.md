# Exploratory designs (E1–E8) — pra-final

Folder ini berisi rancangan **eksplorasi pra-final** yang dilaporkan di **Lampiran A** buku TA (label ilmiah **E1–E8**).

**Bukan** kode evaluasi final Bab 4. Kode official final ada di folder `../src/` (`Varian1` … `Varian4_KDFP`).

Metrik trial (NL/DU/SAC/time/memory, 100 run) dipakai untuk menjustifikasi pergeseran ke desain affine-equivalent final; rancangan E1–E8 **tidak** dilanjutkan sebagai klaim utama.

## Mapping label → file

| Label | File | Ringkas | Legacy name |
|-------|------|---------|-------------|
| **E1** | `E1_Fusion_MaterialMix.java` | Fusi tahap: shuffle digabung dengan pencampuran material (pra-final) | `Varian3_Fusion.java` |
| **E2** | `E2_Stage1_KeyedByteMixing.java` | Keyed byte-mixing pada material hasil titik EC (pra-final) | `Varian3_Stage1.java` |
| **E3** | `E3_RNG_CoordinateExtract.java` | Ekstraksi koordinat dikendalikan RNG (pra-final) | `Varian3_New.java` |
| **E4** | `E4_LightPerturbation.java` | Seperti E3 + perturbasi ringan pada material (pra-final) | `Varian3_New_v2.java` |
| **E5** | `E5_InterleaveMaterial.java` | Interleave material rasio tetap (pra-final) | `Varian3_New_v3A1.java` |
| **E6** | `E6_FinalPermute_RNG.java` | Permutasi akhir berbasis RNG/keyed (pra-final) | `Varian4_Stage2.java` |
| **E7** | `E7_FinalPermute_Revised.java` | Permutasi akhir berbasis RNG dengan revisi parameter (pra-final) | `Varian4_Stage2_Revised.java` |
| **E8** | `E8_HybridPermute_RNG.java` | Permutasi hibrida dengan injeksi RNG (pra-final) | `Varian4B_Stage2.java` |

## Cara compile (contoh, butuh Bouncy Castle)

```bash
# dari root repo
javac -cp lib/bcprov-jdk18on-1.83.jar exploratory/E1_Fusion_MaterialMix.java
```

Beberapa file mungkin bergantung pada utilitas/metrics bersama yang tidak disalin; folder ini diprioritaskan sebagai **arsip rancangan** yang selaras penamaan E1–E8 di buku, bukan harness production.

## Naming rule

- Prefix wajib: `E1_` … `E8_`
- **Tidak** memakai nama `Varian3_*` / `Varian4_*` pada file yang dipush di folder ini
