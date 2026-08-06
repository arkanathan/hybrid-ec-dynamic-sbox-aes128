# Summary metrics exploratory E1–E8 (100 run)

**Bukan** hasil Bab 4 official. Metrik: NL (vectorial), DU, SAC, generation time, memory footprint.  
Kode sumber: folder [`../exploratory/`](../exploratory/) (nama file ber-prefix **E1–E8**).  
Metrik ini selaras **Lampiran B** naskah TA.

| Label | File kode | NL avg | NL min | NL max | DU avg | DU min | DU max | Time avg (ms) | Time min | Time max | Mem avg (KB) | Mem min | Mem max |
|:-----:|:----------|------:|------:|------:|------:|------:|------:|-------------:|---------:|---------:|-------------:|--------:|--------:|
| **E1** | `E1_Fusion_MaterialMix.java` | 92,6 | 84 | 96 | 11,3 | 10 | 18 | 186,01 | 120,47 | 285,32 | 22926,85 | 5215,04 | 57425,90 |
| **E2** | `E2_Stage1_KeyedByteMixing.java` | 92,8 | 86 | 96 | 11,3 | 10 | 14 | 420,40 | 309,40 | 691,38 | 52735,17 | 16043,98 | 78634,50 |
| **E3** | `E3_RNG_CoordinateExtract.java` | 92,4 | 86 | 96 | 11,4 | 10 | 14 | 394,37 | 318,43 | 722,32 | 55181,04 | 8162,34 | 70268,80 |
| **E4** | `E4_LightPerturbation.java` | 92,6 | 88 | 96 | 11,2 | 10 | 14 | 425,90 | 314,17 | 699,95 | 52169,46 | 17451,52 | 84453,55 |
| **E5** | `E5_InterleaveMaterial.java` | 92,8 | 88 | 96 | 11,5 | 10 | 14 | 455,80 | 303,94 | 684,07 | 52467,44 | 32491,86 | 74108,63 |
| **E6** | `E6_FinalPermute_RNG.java` | 92,7 | 86 | 96 | 11,2 | 10 | 14 | 351,91 | 304,87 | 775,10 | 50639,33 | 29776,69 | 86542,31 |
| **E7** | `E7_FinalPermute_Revised.java` | 92,4 | 84 | 98 | 11,2 | 10 | 14 | 407,61 | 304,75 | 692,67 | 50186,44 | 5735,42 | 67268,16 |
| **E8** | `E8_HybridPermute_RNG.java` | 88,3 | 76 | 96 | 11,4 | 10 | 18 | 389,57 | 314,62 | 783,36 | 55936,23 | 20019,70 | 83778,16 |

**Interpretasi:** seluruh rancangan E1–E8 memiliki NL rata-rata ~88–93 dan DU rata-rata ~11–12, jauh di bawah paritas final V1/V3/V4 (NL 112, DU 4). Rancangan trial tidak dilanjutkan; desain final memakai konstruksi affine-equivalent (CBPE/KDFP) di `src/`.
