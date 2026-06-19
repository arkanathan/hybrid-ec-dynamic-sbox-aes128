# Upload ke GitHub

**Nama repo yang dipakai:** `hybrid-ec-dynamic-sbox-aes128`

Deskripsi singkat (untuk GitHub):
> Java implementation of hybrid elliptic-curve dynamic S-box for AES-128 with CBPE and KDFP variants.

## Langkah 1 — Login GitHub (sekali saja)

Buka PowerShell, lalu:

```powershell
gh auth login
```

Pilih:
1. `GitHub.com`
2. `HTTPS`
3. `Login with a web browser` (paling mudah)
4. Copy kode yang muncul, buka browser, paste, authorize

## Langkah 2 — Buat repo & push (otomatis)

```powershell
cd "C:\Improvement of Elliptic Curve-Based Dynamic S-Box for AES-128 Using Random Number Generator\hybrid-ec-dynamic-sbox-aes128"

gh repo create hybrid-ec-dynamic-sbox-aes128 --public --source=. --remote=origin --description "Hybrid EC dynamic S-box for AES-128 (CBPE & KDFP) — Telkom University thesis implementation" --push
```

Kalau mau repo **private** (hanya yang punya link bisa lihat), ganti `--public` jadi `--private`.

## Langkah 3 — Kirim link ke dospem

Setelah sukses, link repo:
```
https://github.com/<username-anda>/hybrid-ec-dynamic-sbox-aes128
```

Cek username:
```powershell
gh api user --jq .login
```

## Alternatif (tanpa gh) — manual di browser

1. Buka https://github.com/new
2. Repository name: `hybrid-ec-dynamic-sbox-aes128`
3. Public atau Private
4. **Jangan** centang "Add README" (sudah ada lokal)
5. Create repository
6. Jalankan perintah yang GitHub tampilkan:

```powershell
cd "C:\Improvement of Elliptic Curve-Based Dynamic S-Box for AES-128 Using Random Number Generator\hybrid-ec-dynamic-sbox-aes128"
git remote add origin https://github.com/<username>/hybrid-ec-dynamic-sbox-aes128.git
git branch -M main
git push -u origin main
```