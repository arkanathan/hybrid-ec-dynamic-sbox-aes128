# BAB 4 — Benchmark Results (100 Runs, Revised Varian 3 & 4)

**Date:** 18 June 2026  
**Environment:** Java + Bouncy Castle (`bcprov-jdk18on-1.83.jar`)  
**Curve:** secp256r1 | **L:** 4096 | **D candidates:** 32 (NL-first selection)  
**NL metric:** Full vectorial nonlinearity (255 non-zero output masks)

## Summary Table

| Variant | Description | NL (avg) | SAC (avg) | DU (avg) | Time (ms) | Memory (KB) |
|:-------:|-------------|:--------:|:---------:|:--------:|:---------:|:-----------:|
| **1** | Static AES-128 baseline | 112.0 | 0.5047 | 4.0 | 0.85 | 18,689 |
| **2** | EC baseline (official) | 92.8 | 0.5028 | 11.2 | 687.31 | 66,327 |
| **3** | CBPE — input affine keyed by h0 | **112.0** | 0.5000 | **4.0** | 699.70 | 33,190 |
| **4** | KDFP — output affine keyed by h1 | **112.0** | 0.5000 | **4.0** | 738.20 | 47,404 |

## Delta vs Varian 2

| Variant | ΔNL | ΔDU | ΔTime | Result |
|:-------:|:---:|:---:|:-----:|--------|
| 3 | **+19.2** | **−7.2** | +12 ms | Clear cryptographic improvement |
| 4 | **+19.2** | **−7.2** | +51 ms | Clear cryptographic improvement |

## Design Changes Implemented

### Varian 4 (KDFP Revised)
- **Formula:** `S(x) = A_k(Inv(B_k·x ⊕ b_k)) ⊕ c_k`
- **h1 (R-point coordinates)** primarily drives output affine parameters `(A_k, c_k)`
- **NL-first D selection** across 32 candidates

### Varian 3 (CBPE Revised)
- Same affine-equivalent core as Varian 4
- **h0 (original EC coordinates)** primarily drives input affine parameters `(B_k, b_k)`
- **NL-first D selection** across 32 candidates

### Important finding (document in thesis)
Applying `Inv` **after** a Fisher–Yates permutation (`A(Inv(π(x)))`) does **not** preserve high nonlinearity because arbitrary permutations are not affine maps. NL remained ~96. The working design uses EC material to **key affine-equivalent parameters** rather than to add more shuffle layers.

## Raw Logs

- `benchmark_varian1_rev2.log`
- `benchmark_varian2_rev2.log`
- `benchmark_varian3_rev2.log`
- `benchmark_varian4_rev2.log`