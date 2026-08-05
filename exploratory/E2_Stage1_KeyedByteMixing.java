// Exploratory / pra-final design — label E2 (Lampiran A).
// Bukan rancangan official Bab 4. Official: src/Varian1..Varian4_*.java
// Legacy source class name: E2_Stage1_KeyedByteMixing
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class E2_Stage1_KeyedByteMixing {

    public static void main(String[] args) throws Exception {
        String keyHex = "0123456789abcdef0123456789abcdef";
        int L = 4096;
        int runs = 50;
        int numDCandidates = 8;

        System.out.println("🚀 VARIAN 3: Stage 1 – EC-Based Keyed Byte-Mixing Improvement (Revised Final)");
        System.out.println("L = " + L + " | D Candidates = " + numDCandidates + " | Runs = " + runs);
        System.out.println("=".repeat(95));

        double sumNL = 0, sumSAC = 0, sumTime = 0;

        for (int run = 0; run < runs; run++) {
            long startTime = System.nanoTime();

            // Setup EC curve (sama dengan Varian 2)
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            BigInteger p = spec.getCurve().getField().getCharacteristic();
            BigInteger order = spec.getN();
            BigInteger k = new BigInteger(keyHex, 16).mod(order);
            ECPoint G = spec.getG();

            // Generate point sequence
            List<ECPoint> points = new ArrayList<>();
            ECPoint P = G;
            for (int i = 0; i < L; i++) {
                points.add(P);
                P = nextPoint(P, spec, p);
            }

            // Deterministic D optimization
            ECPoint bestD = null;
            double bestEntropy = -1.0;
            for (int cand = 0; cand < numDCandidates; cand++) {
                ECPoint candidateD = G.multiply(new BigInteger(order.bitLength(), new SecureRandom())).normalize();
                List<ECPoint> R_points = generateRPoints(points, candidateD);
                double entropy = calculateEntropy(R_points);
                if (entropy > bestEntropy) {
                    bestEntropy = entropy;
                    bestD = candidateD;
                }
            }

            // Q_points and R_points
            List<ECPoint> Q_points = new ArrayList<>();
            for (ECPoint pt : points) {
                Q_points.add(pt.add(G.multiply(k.multiply(BigInteger.valueOf(2)))));
            }
            List<ECPoint> R_points = generateRPoints(points, bestD);

            // === STAGE 1 REVISED: Keyed Byte-Mixing (h0 x-dominant, h1 y-dominant) ===
            List<Integer> h0 = keyedByteMixing(Q_points, k, true);   // x-dominant
            List<Integer> h1 = keyedByteMixing(R_points, k, false);  // y-dominant

            // Two-stage Fisher-Yates
            int[] S0 = new int[256];
            for (int i = 0; i < 256; i++) S0[i] = i;

            int[] S1 = fisherYatesShuffle(S0.clone(), h0);
            int[] S2 = fisherYatesShuffle(S1, h1);

            double nl = calculateNonlinearity(S2);
            double sac = calculateSAC(S2, 1000);

            long endTime = System.nanoTime();
            double execTimeMs = (endTime - startTime) / 1_000_000.0;

            boolean bijective = isBijective(S2);

            System.out.printf("Run %d: NL=%3d | SAC=%.4f | Time=%6.2f ms | Bijective=%b | Best D Entropy=%.4f%n",
                    run + 1, (int) nl, sac, execTimeMs, bijective, bestEntropy);

            sumNL += nl;
            sumSAC += sac;
            sumTime += execTimeMs;
        }

        System.out.println("\n" + "=".repeat(95));
        System.out.println("=== VARIAN 3 AVERAGE - Stage 1 EC-Based Keyed Byte-Mixing Improvement (Revised Final) ===");
        System.out.printf("NL          : %.1f%n", sumNL / runs);
        System.out.printf("SAC         : %.4f%n", sumSAC / runs);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / runs);
        System.out.println("=".repeat(95));
    }

    // ==================== HELPER METHODS (identik dengan Varian 2) ====================
    private static ECPoint nextPoint(ECPoint P, ECNamedCurveParameterSpec spec, BigInteger p) {
        BigInteger y = P.getYCoord().toBigInteger();
        if (y.compareTo(p.divide(BigInteger.valueOf(2))) < 0) {
            return P.negate();
        }
        BigInteger x = P.getXCoord().toBigInteger().add(BigInteger.ONE);
        BigInteger a = spec.getCurve().getA().toBigInteger();
        BigInteger b = spec.getCurve().getB().toBigInteger();
        while (true) {
            BigInteger rhs = x.pow(3).add(a.multiply(x)).add(b).mod(p);
            BigInteger legendre = rhs.modPow(p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)), p);
            if (legendre.equals(BigInteger.ONE)) {
                BigInteger yNew = rhs.modPow(p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p);
                return spec.getCurve().createPoint(x, yNew);
            }
            x = x.add(BigInteger.ONE);
        }
    }

    private static List<ECPoint> generateRPoints(List<ECPoint> points, ECPoint D) {
        List<ECPoint> R = new ArrayList<>();
        for (ECPoint q : points) R.add(q.add(D));
        return R;
    }

    // STAGE 1 REVISED: Keyed Byte-Mixing dengan h0 x-dominant & h1 y-dominant
    private static List<Integer> keyedByteMixing(List<ECPoint> points, BigInteger k, boolean isH0) {
        List<Integer> mixed = new ArrayList<>();
        byte[] keyBytes = k.toByteArray();
        for (int idx = 0; idx < points.size(); idx++) {
            ECPoint pt = points.get(idx);
            byte[] xBytes = pt.getXCoord().toBigInteger().toByteArray();
            byte[] yBytes = pt.getYCoord().toBigInteger().toByteArray();

            int startX = Math.max(0, xBytes.length - 16);
            int startY = Math.max(0, yBytes.length - 16);

            // 8 bytes dominant + 4 bytes supporting
            int dominantStart = isH0 ? startX : startY;
            int supportStart  = isH0 ? startY : startX;
            byte[] dominantBytes = isH0 ? xBytes : yBytes;
            byte[] supportBytes  = isH0 ? yBytes : xBytes;

            for (int i = 0; i < 8; i++) {   // dominant bytes
                int bDom = (int) dominantBytes[dominantStart + i] & 0xFF;
                int bSup = (int) supportBytes[supportStart + (i % 4)] & 0xFF;
                int m = bDom ^ bSup;                                      // XOR
                int rot = keyBytes[idx % keyBytes.length] & 0x07;
                m = Integer.rotateLeft(m, rot);
                int offset = (idx % 256) + (keyBytes[(idx + 11) % keyBytes.length] & 0xFF);
                m = (m + offset) & 0xFF;                                  // addition index-dependent
                mixed.add(m);
            }
            // Tambah 4 bytes supporting tambahan agar richness mendekati baseline
            for (int i = 0; i < 4; i++) {
                int bSup = (int) supportBytes[supportStart + i] & 0xFF;
                mixed.add(bSup);
            }
        }
        return mixed;
    }

    private static int[] fisherYatesShuffle(int[] sbox, List<Integer> randomSource) {
        int[] s = sbox.clone();
        for (int i = s.length - 1; i > 0; i--) {
            int j = randomSource.get(i % randomSource.size()) % (i + 1);
            int temp = s[i];
            s[i] = s[j];
            s[j] = temp;
        }
        return s;
    }

    private static double calculateNonlinearity(int[] sbox) {
        int minNL = 128;
        for (int bit = 0; bit < 8; bit++) {
            int[] f = new int[256];
            for (int x = 0; x < 256; x++) {
                f[x] = 1 - 2 * ((sbox[x] >> bit) & 1);
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

    private static boolean isBijective(int[] sbox) {
        Set<Integer> set = new HashSet<>();
        for (int v : sbox) set.add(v);
        return set.size() == 256;
    }

    private static double calculateEntropy(List<ECPoint> R_points) {
        int[] count = new int[256];
        for (ECPoint r : R_points) {
            byte[] xBytes = r.getXCoord().toBigInteger().toByteArray();
            int val = (int) xBytes[xBytes.length - 1] & 0xFF;
            count[val]++;
        }
        double entropy = 0.0;
        for (int c : count) {
            if (c > 0) {
                double p = (double) c / R_points.size();
                entropy -= p * Math.log(p) / Math.log(2);
            }
        }
        return entropy;
    }
}