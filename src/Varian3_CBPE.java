// Nathan Dava Arkananta — NIM 1301223297
// Varian 3: CBPE (Coordinate-Based Parameter Extraction)
// Konstruksi affine-equivalent; parameter input (B_k, b_k) diturunkan dari h0
// Tugas Akhir — Hybrid EC Dynamic S-Box untuk AES-128
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class Varian3_CBPE {

    private static final String CURVE_NAME = "secp256r1";
    private static final int L = 4096;
    private static final int NUM_D_CANDIDATES = 32;
    private static final int RUNS = 100;
    private static final int WARMUP_RUNS = 10;
    private static final String BASE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef" +
                                           "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static final int[] GF256_INV = buildGf256InvTable();
    private static final int[] AES_AFFINE_MATRIX = {
        0xF1, 0xE3, 0xC7, 0x8F, 0x1F, 0x3E, 0x7C, 0xF8
    };

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 VARIAN 3: EC-Keyed Affine-Equivalent Input Layer (CBPE Revised)");
        System.out.println("S(x) = A_k(Inv(B_k·x ⊕ b_k)) ⊕ c_k | Input affine (B,b) driven by h0 / coordinates");
        System.out.println("D Selection = NL↑ → DU↓ → |SAC-0.5|↓ | Candidates = " + NUM_D_CANDIDATES + " | Runs = " + RUNS);
        System.out.println("=".repeat(130));

        System.out.println("Melakukan warm-up...");
        for (int i = 0; i < WARMUP_RUNS; i++) {
            constructSBox(generateDeterministicKey(BASE_KEY, i));
        }
        System.out.println("Warm-up selesai.\n");

        double sumNL = 0, sumSAC = 0, sumDU = 0, sumTime = 0, sumMemory = 0;

        for (int run = 0; run < RUNS; run++) {
            String sessionKey = generateDeterministicKey(BASE_KEY, run);

            System.gc();
            Thread.sleep(50);

            long beforeMemory = getUsedMemory();
            long startTime = System.nanoTime();

            int[] sbox = constructSBox(sessionKey);

            long endTime = System.nanoTime();
            long afterMemory = getUsedMemory();

            double execTimeMs = (endTime - startTime) / 1_000_000.0;
            long memoryUsed = Math.max(0, afterMemory - beforeMemory);

            double nl = calculateNonlinearity(sbox);
            double sac = calculateSACExhaustive(sbox);
            double du = calculateDifferentialUniformity(sbox);
            boolean bijective = isBijective(sbox);

            System.out.printf(
                "Run %4d: NL=%3d | SAC=%.4f | DU=%2d | Time=%6.2f ms | Memory=%8.2f KB | Bijective=%b%n",
                run + 1, (int) nl, sac, (int) du, execTimeMs, memoryUsed / 1024.0, bijective
            );

            sumNL += nl;
            sumSAC += sac;
            sumDU += du;
            sumTime += execTimeMs;
            sumMemory += memoryUsed;
        }

        System.out.println("\n" + "=".repeat(130));
        System.out.println("=== VARIAN 3 AVERAGE (" + RUNS + " Runs) ===");
        System.out.printf("NL          : %.1f%n", sumNL / RUNS);
        System.out.printf("SAC         : %.4f%n", sumSAC / RUNS);
        System.out.printf("DU          : %.1f%n", sumDU / RUNS);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / RUNS);
        System.out.printf("Memory      : %.2f KB%n", (sumMemory / RUNS) / 1024.0);
        System.out.println("=".repeat(130));
    }

    private static int[] constructSBox(String sessionKey) throws Exception {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
        BigInteger p = spec.getCurve().getField().getCharacteristic();
        BigInteger order = spec.getN();
        ECPoint G = spec.getG();
        BigInteger k = new BigInteger(sessionKey, 16).mod(order);

        List<ECPoint> points = generatePointSequence(G, spec, p, L);
        List<Integer> h0 = extractMaterialBytes(points, G, k);

        int[] bestSbox = null;
        int bestNl = -1;
        int bestDu = Integer.MAX_VALUE;
        double bestSacDist = Double.MAX_VALUE;

        for (int i = 0; i < NUM_D_CANDIDATES; i++) {
            ECPoint D = generateDeterministicD(spec, order, sessionKey, i);
            List<ECPoint> rPoints = generateRPoints(points, D);
            List<Integer> h1 = extractMaterialBytes(rPoints, null, null);

            int[] sbox = buildInputKeyedAffineEquivalentSBox(sessionKey, h0, h1, i);

            int nl = (int) calculateNonlinearity(sbox);
            int du = (int) calculateDifferentialUniformity(sbox);
            double sac = calculateSACExhaustive(sbox);
            double sacDist = Math.abs(sac - 0.5);

            if (bestSbox == null || isBetterScore(nl, du, sacDist, bestNl, bestDu, bestSacDist)) {
                bestSbox = sbox;
                bestNl = nl;
                bestDu = du;
                bestSacDist = sacDist;
            }
        }

        return bestSbox;
    }

    // CBPE: h0 mengekang parameter affine sisi masukan (B_k, b_k).
    private static int[] buildInputKeyedAffineEquivalentSBox(String sessionKey,
                                                             List<Integer> h0,
                                                             List<Integer> h1,
                                                             int dIndex) throws Exception {
        byte[] inputSeed = MessageDigest.getInstance("SHA-256").digest(
                (sessionKey + "v3input" + dIndex + materialFingerprint(h0, 96))
                        .getBytes(StandardCharsets.UTF_8)
        );

        int[] inputMatrix = deriveInvertibleMatrix(inputSeed, 0);
        int inputConstant = (inputSeed[16] ^ mixList(h0)) & 0xFF;

        byte[] outputSeed = deriveSeed(sessionKey, "v3output", h0, h1, dIndex);
        int[] outputMatrix = deriveInvertibleMatrix(outputSeed, 0);
        int outputConstant = outputSeed[17] & 0xFF;

        return buildAffineEquivalentSBox(inputMatrix, inputConstant, outputMatrix, outputConstant);
    }

    private static int[] buildAffineEquivalentSBox(int[] inputMatrix,
                                                   int inputConstant,
                                                   int[] outputMatrix,
                                                   int outputConstant) {
        int[] sbox = new int[256];
        for (int x = 0; x < 256; x++) {
            int linearInput = applyAffine(inputMatrix, inputConstant, x);
            int inverted = gfInverse(linearInput);
            sbox[x] = applyAffine(outputMatrix, outputConstant, inverted);
        }
        return sbox;
    }

    private static byte[] deriveSeed(String sessionKey,
                                     String label,
                                     List<Integer> h0,
                                     List<Integer> h1,
                                     int dIndex) throws Exception {
        String payload = sessionKey + label + dIndex + materialFingerprint(h0, 32) + materialFingerprint(h1, 32);
        return MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String materialFingerprint(List<Integer> material, int limit) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(limit, material.size());
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%02x", material.get(i)));
        }
        return sb.toString();
    }

    private static int mixList(List<Integer> material) {
        int mix = 0;
        int count = Math.min(96, material.size());
        for (int i = 0; i < count; i++) {
            mix ^= material.get(i);
            mix = Integer.rotateLeft(mix, 5);
        }
        return mix & 0xFF;
    }

    private static boolean isBetterScore(int nlA, int duA, double sacDistA,
                                         int nlB, int duB, double sacDistB) {
        if (nlA != nlB) return nlA > nlB;
        if (duA != duB) return duA < duB;
        return sacDistA < sacDistB;
    }

    private static ECPoint generateDeterministicD(ECNamedCurveParameterSpec spec,
                                                  BigInteger order,
                                                  String sessionKey,
                                                  int candidateIndex) throws Exception {
        String seed = sessionKey + "D" + candidateIndex;
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
        BigInteger dScalar = new BigInteger(1, hash).mod(order);
        if (dScalar.equals(BigInteger.ZERO)) dScalar = BigInteger.ONE;
        return spec.getG().multiply(dScalar).normalize();
    }

        private static int[] buildGf256InvTable() {
        int[] inv = new int[256];
        inv[0] = 0;
        for (int a = 1; a < 256; a++) {
            inv[a] = gfPow(a, 254);
        }
        return inv;
    }

    private static int gfPow(int base, int exp) {
        int result = 1;
        int b = base;
        int e = exp;
        while (e > 0) {
            if ((e & 1) == 1) result = gfMul(result, b);
            b = gfMul(b, b);
            e >>= 1;
        }
        return result;
    }

    private static int gfMul(int a, int b) {
        int p = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) p ^= a;
            boolean hi = (a & 0x80) != 0;
            a <<= 1;
            if (hi) a ^= 0x11B;
            b >>= 1;
        }
        return p & 0xFF;
    }

    private static int gfInverse(int a) {
        return GF256_INV[a & 0xFF];
    }

    private static int applyAffine(int[] matrixRows, int constant, int value) {
        int out = 0;
        for (int row = 0; row < 8; row++) {
            int bit = Integer.bitCount(matrixRows[row] & value) & 1;
            if (bit == 1) out |= (1 << row);
        }
        return (out ^ constant) & 0xFF;
    }

    private static int[] deriveInvertibleMatrix(byte[] hash, int offset) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int[] rows = new int[8];
            for (int r = 0; r < 8; r++) {
                int base = AES_AFFINE_MATRIX[(r + attempt) % 8];
                int mask = hash[(offset + r + attempt) % hash.length] & 0xFF;
                rows[r] = (base ^ mask) & 0xFF;
                rows[r] |= (1 << r);
            }
            if (matrixRank(rows) == 8) return rows;
        }
        return AES_AFFINE_MATRIX.clone();
    }

    private static int matrixRank(int[] rows) {
        int[][] m = new int[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                m[r][c] = (rows[r] >> c) & 1;
            }
        }

        int pivotRow = 0;
        for (int col = 0; col < 8; col++) {
            int pivot = -1;
            for (int row = pivotRow; row < 8; row++) {
                if (m[row][col] == 1) {
                    pivot = row;
                    break;
                }
            }
            if (pivot == -1) continue;

            if (pivot != pivotRow) {
                int[] tmp = m[pivot];
                m[pivot] = m[pivotRow];
                m[pivotRow] = tmp;
            }

            for (int row = 0; row < 8; row++) {
                if (row != pivotRow && m[row][col] == 1) {
                    for (int c = 0; c < 8; c++) {
                        m[row][c] ^= m[pivotRow][c];
                    }
                }
            }
            pivotRow++;
        }
        return pivotRow;
    }

        private static String generateDeterministicKey(String baseKey, int run) throws Exception {
        String seed = baseKey + run;
        byte[] hash1 = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] hash2 = MessageDigest.getInstance("SHA-256").digest((seed + "salt").getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash1) + bytesToHex(hash2);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static List<ECPoint> generatePointSequence(ECPoint G, ECNamedCurveParameterSpec spec,
                                                       BigInteger p, int length) {
        List<ECPoint> points = new ArrayList<>();
        ECPoint P = G;
        for (int i = 0; i < length; i++) {
            points.add(P);
            P = nextPoint(P, spec, p);
        }
        return points;
    }

    private static ECPoint nextPoint(ECPoint P, ECNamedCurveParameterSpec spec, BigInteger p) {
        BigInteger y = P.getYCoord().toBigInteger();
        if (y.compareTo(p.divide(BigInteger.valueOf(2))) < 0) return P.negate();

        BigInteger x = P.getXCoord().toBigInteger().add(BigInteger.ONE);
        BigInteger a = spec.getCurve().getA().toBigInteger();
        BigInteger b = spec.getCurve().getB().toBigInteger();

        while (true) {
            BigInteger rhs = x.pow(3).add(a.multiply(x)).add(b).mod(p);
            if (rhs.modPow(p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)), p).equals(BigInteger.ONE)) {
                BigInteger yNew = rhs.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p);
                return spec.getCurve().createPoint(x, yNew);
            }
            x = x.add(BigInteger.ONE);
        }
    }

    private static List<ECPoint> generateRPoints(List<ECPoint> points, ECPoint D) {
        List<ECPoint> R = new ArrayList<>();
        for (ECPoint pt : points) R.add(pt.add(D));
        return R;
    }

    private static List<Integer> extractMaterialBytes(List<ECPoint> points, ECPoint G, BigInteger k) {
        List<Integer> bytes = new ArrayList<>();
        for (ECPoint pt : points) {
            ECPoint target = (G != null && k != null) ? pt.add(G.multiply(k.multiply(BigInteger.valueOf(2)))) : pt;
            byte[] xBytes = target.getXCoord().toBigInteger().toByteArray();
            for (int j = Math.max(0, xBytes.length - 16); j < xBytes.length; j++) {
                bytes.add((int) xBytes[j] & 0xFF);
            }
        }
        return bytes;
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

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

    private static double calculateSACExhaustive(int[] sbox) {
        int changes = 0;
        int total = 0;
        for (int x = 0; x < 256; x++) {
            for (int bit = 0; bit < 8; bit++) {
                int x2 = x ^ (1 << bit);
                changes += Integer.bitCount(sbox[x] ^ sbox[x2]);
                total += 8;
            }
        }
        return changes / (double) total;
    }

    private static double calculateDifferentialUniformity(int[] sbox) {
        int[][] diffTable = new int[256][256];
        for (int x = 0; x < 256; x++) {
            for (int dx = 0; dx < 256; dx++) {
                diffTable[dx][sbox[x] ^ sbox[x ^ dx]]++;
            }
        }
        int maxFreq = 0;
        for (int dx = 1; dx < 256; dx++) {
            for (int dy = 0; dy < 256; dy++) {
                if (diffTable[dx][dy] > maxFreq) maxFreq = diffTable[dx][dy];
            }
        }
        return maxFreq;
    }

    private static boolean isBijective(int[] sbox) {
        Set<Integer> set = new HashSet<>();
        for (int v : sbox) set.add(v);
        return set.size() == 256;
    }
}