// Nathan Dava Arkananta — NIM 1301223297
// Varian 1: S-box statis AES-128 (baseline pembanding)
// Tugas Akhir — Hybrid EC Dynamic S-Box untuk AES-128
import java.util.*;

public class Varian1_StaticAES {

    private static final int[] AES_SBOX = {
        0x63,0x7C,0x77,0x7B,0xF2,0x6B,0x6F,0xC5,0x30,0x01,0x67,0x2B,0xFE,0xD7,0xAB,0x76,
        0xCA,0x82,0xC9,0x7D,0xFA,0x59,0x47,0xF0,0xAD,0xD4,0xA2,0xAF,0x9C,0xA4,0x72,0xC0,
        0xB7,0xFD,0x93,0x26,0x36,0x3F,0xF7,0xCC,0x34,0xA5,0xE5,0xF1,0x71,0xD8,0x31,0x15,
        0x04,0xC7,0x23,0xC3,0x18,0x96,0x05,0x9A,0x07,0x12,0x80,0xE2,0xEB,0x27,0xB2,0x75,
        0x09,0x83,0x2C,0x1A,0x1B,0x6E,0x5A,0xA0,0x52,0x3B,0xD6,0xB3,0x29,0xE3,0x2F,0x84,
        0x53,0xD1,0x00,0xED,0x20,0xFC,0xB1,0x5B,0x6A,0xCB,0xBE,0x39,0x4A,0x4C,0x58,0xCF,
        0xD0,0xEF,0xAA,0xFB,0x43,0x4D,0x33,0x85,0x45,0xF9,0x02,0x7F,0x50,0x3C,0x9F,0xA8,
        0x51,0xA3,0x40,0x8F,0x92,0x9D,0x38,0xF5,0xBC,0xB6,0xDA,0x21,0x10,0xFF,0xF3,0xD2,
        0xCD,0x0C,0x13,0xEC,0x5F,0x97,0x44,0x17,0xC4,0xA7,0x7E,0x3D,0x64,0x5D,0x19,0x73,
        0x60,0x81,0x4F,0xDC,0x22,0x2A,0x90,0x88,0x46,0xEE,0xB8,0x14,0xDE,0x5E,0x0B,0xDB,
        0xE0,0x32,0x3A,0x0A,0x49,0x06,0x24,0x5C,0xC2,0xD3,0xAC,0x62,0x91,0x95,0xE4,0x79,
        0xE7,0xC8,0x37,0x6D,0x8D,0xD5,0x4E,0xA9,0x6C,0x56,0xF4,0xEA,0x65,0x7A,0xAE,0x08,
        0xBA,0x78,0x25,0x2E,0x1C,0xA6,0xB4,0xC6,0xE8,0xDD,0x74,0x1F,0x4B,0xBD,0x8B,0x8A,
        0x70,0x3E,0xB5,0x66,0x48,0x03,0xF6,0x0E,0x61,0x35,0x57,0xB9,0x86,0xC1,0x1D,0x9E,
        0xE1,0xF8,0x98,0x11,0x69,0xD9,0x8E,0x94,0x9B,0x1E,0x87,0xE9,0xCE,0x55,0x28,0xDF,
        0x8C,0xA1,0x89,0x0D,0xBF,0xE6,0x42,0x68,0x41,0x99,0x2D,0x0F,0xB0,0x54,0xBB,0x16
    };

    private static final int RUNS = 100;           // 100 run sesuai desain eksperimen BAB 4
    private static final int SAC_TRIALS = 10000;   // Sampling SAC untuk baseline statis

    public static void main(String[] args) {
        System.out.println("🚀 VARIAN 1: Static AES-128 S-box Baseline");
        System.out.println("Runs = " + RUNS);
        System.out.println("=".repeat(100));

        double sumNL = 0, sumSAC = 0, sumDU = 0, sumTime = 0, sumMemory = 0;

        for (int run = 0; run < RUNS; run++) {
            long startTime = System.nanoTime();

            // Salin tabel S-box AES statis
            int[] sbox = AES_SBOX.clone();

            // Evaluasi
            double nl = calculateNonlinearity(sbox);
            double sac = calculateSAC(sbox, SAC_TRIALS);
            double du = calculateDifferentialUniformity(sbox);
            boolean bijective = isBijective(sbox);

            long endTime = System.nanoTime();
            double execTimeMs = (endTime - startTime) / 1_000_000.0;

            // Estimasi penggunaan memori
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            System.out.printf("Run %3d: NL=%3d | SAC=%.4f | DU=%2d | Time=%6.4f ms | Bijective=%b%n",
                    run + 1, (int) nl, sac, (int) du, execTimeMs, bijective);

            sumNL += nl;
            sumSAC += sac;
            sumDU += du;
            sumTime += execTimeMs;
            sumMemory += usedMemory;
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("=== VARIAN 1 AVERAGE - Static AES-128 S-box Baseline ===");
        System.out.printf("NL          : %.1f%n", sumNL / RUNS);
        System.out.printf("SAC         : %.4f%n", sumSAC / RUNS);
        System.out.printf("DU          : %.1f%n", sumDU / RUNS);
        System.out.printf("Waktu       : %.4f ms%n", sumTime / RUNS);
        System.out.printf("Memory (avg): %.2f KB%n", (sumMemory / RUNS) / 1024.0);
        System.out.println("=".repeat(100));
    }

        // Nonlinearity vektorial: minimum dari 255 mask bit keluaran.
    private static double calculateNonlinearity(int[] sbox) {
        int minNL = 128;
        for (int mask = 1; mask < 256; mask++) {
            int[] f = new int[256];
            for (int x = 0; x < 256; x++) {
                int bit = Integer.bitCount(mask & sbox[x]) & 1;
                f[x] = 1 - 2 * bit;
            }
            int maxW = fwht(f);
            int nl = 128 - (maxW / 2);
            if (nl < minNL) minNL = nl;
        }
        return minNL;
    }

    private static int fwht(int[] f) {
        int[] h = f.clone();
        for (int i = 1; i < 256; i <<= 1) {
            for (int j = 0; j < 256; j += 2 * i) {
                for (int k = 0; k < i; k++) {
                    int x = h[j + k];
                    int y = h[j + k + i];
                    h[j + k] = x + y;
                    h[j + k + i] = x - y;
                }
            }
        }
        int max = 0;
        for (int v : h) if (Math.abs(v) > max) max = Math.abs(v);
        return max;
    }

    private static double calculateSAC(int[] sbox, int trials) {
        int changes = 0;
        Random rand = new Random();
        for (int t = 0; t < trials; t++) {
            int x = rand.nextInt(256);
            int bit = rand.nextInt(8);
            int x2 = x ^ (1 << bit);
            int diff = sbox[x] ^ sbox[x2];
            changes += Integer.bitCount(diff);
        }
        return changes / (double) (trials * 8);
    }

    private static double calculateDifferentialUniformity(int[] sbox) {
        int[][] diffTable = new int[256][256];
        for (int x = 0; x < 256; x++) {
            for (int dx = 0; dx < 256; dx++) {
                int dy = sbox[x] ^ sbox[x ^ dx];
                diffTable[dx][dy]++;
            }
        }
        int maxFreq = 0;
        for (int dx = 1; dx < 256; dx++) { // lewati dx = 0
            for (int dy = 0; dy < 256; dy++) {
                if (diffTable[dx][dy] > maxFreq) {
                    maxFreq = diffTable[dx][dy];
                }
            }
        }
        return maxFreq;
    }

    private static boolean isBijective(int[] sbox) {
        boolean[] seen = new boolean[256];
        for (int v : sbox) {
            if (seen[v]) return false;
            seen[v] = true;
        }
        return true;
    }
}