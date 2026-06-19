// Nathan Dava Arkananta — NIM 1301223297
// Varian 2: Standardized EC Baseline (shuffle-only, Ibrahim & Abbas teradaptasi)
// Tugas Akhir — Hybrid EC Dynamic S-Box untuk AES-128
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class Varian2_StandardizedBaseline {

    private static final String CURVE_NAME = "secp256r1";
    private static final int L = 4096;
    private static final int NUM_D_CANDIDATES = 8;
    private static final int RUNS = 100;
    private static final int WARMUP_RUNS = 10;           // Warm-up
    private static final String BASE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef" +
                                           "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 VARIAN 2: Standardized EC Baseline (Stable Memory Measurement)");
        System.out.println("L = " + L + " | D Candidates = " + NUM_D_CANDIDATES + " | Runs = " + RUNS);
        System.out.println("=".repeat(120));

        // Warm-up JVM
        System.out.println("Melakukan warm-up...");
        for (int i = 0; i < WARMUP_RUNS; i++) {
            String key = generateDeterministicKey(BASE_KEY, i);
            
            // Proses konstruksi S-box (tanpa mencetak hasil)
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            BigInteger p = spec.getCurve().getField().getCharacteristic();
            BigInteger order = spec.getN();
            ECPoint G = spec.getG();
            BigInteger k = new BigInteger(key, 16).mod(order);

            List<ECPoint> points = generatePointSequence(G, spec, p, L);
            ECPoint bestD = findBestDeterministicD(points, spec, order, key, NUM_D_CANDIDATES);
            List<ECPoint> R_points = generateRPoints(points, bestD);

            List<Integer> h0 = extractMaterialBytes(points, G, k);
            List<Integer> h1 = extractMaterialBytes(R_points, null, null);

            int[] S0 = initializeIdentitySBox();
            int[] S1 = fisherYatesShuffle(S0, h0);
            int[] S2 = fisherYatesShuffle(S1, h1);
        }
        System.out.println("Warm-up selesai.\n");

        double sumNL = 0, sumSAC = 0, sumDU = 0, sumTime = 0, sumMemory = 0;

        for (int run = 0; run < RUNS; run++) {
            String sessionKey = generateDeterministicKey(BASE_KEY, run);

            // GC sebelum pengukuran memori
            System.gc();
            Thread.sleep(50);

            long beforeMemory = getUsedMemory();
            long startTime = System.nanoTime();

            // Konstruksi S-box
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            BigInteger p = spec.getCurve().getField().getCharacteristic();
            BigInteger order = spec.getN();
            ECPoint G = spec.getG();
            BigInteger k = new BigInteger(sessionKey, 16).mod(order);

            List<ECPoint> points = generatePointSequence(G, spec, p, L);
            ECPoint bestD = findBestDeterministicD(points, spec, order, sessionKey, NUM_D_CANDIDATES);
            List<ECPoint> R_points = generateRPoints(points, bestD);

            List<Integer> h0 = extractMaterialBytes(points, G, k);
            List<Integer> h1 = extractMaterialBytes(R_points, null, null);

            int[] S0 = initializeIdentitySBox();
            int[] S1 = fisherYatesShuffle(S0, h0);
            int[] S2 = fisherYatesShuffle(S1, h1);

            long endTime = System.nanoTime();
            long afterMemory = getUsedMemory();

            double execTimeMs = (endTime - startTime) / 1_000_000.0;
            long memoryUsed = Math.max(0, afterMemory - beforeMemory);

            // Evaluasi metrik (di luar waktu konstruksi)
            double nl = calculateNonlinearity(S2);
            double sac = calculateSACExhaustive(S2);
            double du = calculateDifferentialUniformity(S2);
            boolean bijective = isBijective(S2);

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

        System.out.println("\n" + "=".repeat(120));
        System.out.println("=== VARIAN 2 AVERAGE (" + RUNS + " Runs) ===");
        System.out.printf("NL          : %.1f%n", sumNL / RUNS);
        System.out.printf("SAC         : %.4f%n", sumSAC / RUNS);
        System.out.printf("DU          : %.1f%n", sumDU / RUNS);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / RUNS);
        System.out.printf("Memory      : %.2f KB%n", (sumMemory / RUNS) / 1024.0);
        System.out.println("=".repeat(120));
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

    private static ECPoint findBestDeterministicD(List<ECPoint> points, ECNamedCurveParameterSpec spec,
                                                  BigInteger order, String sessionKey, int numCandidates) throws Exception {
        ECPoint bestD = null;
        double bestEntropy = -1;

        for (int i = 0; i < numCandidates; i++) {
            String seed = sessionKey + "D" + i;
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
            BigInteger dScalar = new BigInteger(1, hash).mod(order);

            if (dScalar.equals(BigInteger.ZERO)) dScalar = BigInteger.ONE;

            ECPoint D = spec.getG().multiply(dScalar).normalize();
            double entropy = calculateEntropy(generateRPoints(points, D));

            if (entropy > bestEntropy) {
                bestEntropy = entropy;
                bestD = D;
            }
        }
        return bestD;
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

    private static int[] initializeIdentitySBox() {
        int[] sbox = new int[256];
        for (int i = 0; i < 256; i++) sbox[i] = i;
        return sbox;
    }

    private static int[] fisherYatesShuffle(int[] sbox, List<Integer> randomSource) {
        int[] s = sbox.clone();
        int idx = 0;
        int size = randomSource.size();

        for (int i = s.length - 1; i > 0; i--) {
            int byte1 = randomSource.get(idx % size);
            int byte2 = randomSource.get((idx + 1) % size);
            int j = ((byte1 << 8) | byte2) % (i + 1);

            int temp = s[i];
            s[i] = s[j];
            s[j] = temp;
            idx += 2;
        }
        return s;
    }

    private static double calculateEntropy(List<ECPoint> points) {
        int[] count = new int[256];
        for (ECPoint pt : points) {
            byte[] xBytes = pt.getXCoord().toBigInteger().toByteArray();
            int val = (int) xBytes[xBytes.length - 1] & 0xFF;
            count[val]++;
        }
        double entropy = 0.0;
        for (int c : count) {
            if (c > 0) {
                double p = (double) c / points.size();
                entropy -= p * Math.log(p) / Math.log(2);
            }
        }
        return entropy;
    }

    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
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