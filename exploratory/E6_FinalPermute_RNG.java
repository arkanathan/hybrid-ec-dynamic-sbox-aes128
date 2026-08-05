// Exploratory / pra-final design — label E6 (Lampiran A).
// Bukan rancangan official Bab 4. Official: src/Varian1..Varian4_*.java
// Legacy source class name: E6_FinalPermute_RNG
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

public class E6_FinalPermute_RNG {

    public static void main(String[] args) throws Exception {
        String keyHex = "0123456789abcdef0123456789abcdef";
        int L = 4096;
        int runs = 50;
        int numDCandidates = 8;

        System.out.println("🚀 VARIAN 4: Stage 2 – Keyed RNG-Driven Final Permutation");
        System.out.println("L = " + L + " | D Candidates = " + numDCandidates + " | Runs = " + runs);
        System.out.println("=".repeat(95));

        double sumNL = 0, sumSAC = 0, sumTime = 0;

        for (int run = 0; run < runs; run++) {
            long startTime = System.nanoTime();

            // Setup EC curve (sama dengan Varian 2 & 3)
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

            // === STAGE 1 (Revised Final) ===
            List<Integer> h0 = keyedByteMixing(Q_points, k, true);   // x-dominant
            List<Integer> h1 = keyedByteMixing(R_points, k, false);  // y-dominant

            // Two-stage Fisher-Yates → intermediate S-box
            int[] S0 = new int[256];
            for (int i = 0; i < 256; i++) S0[i] = i;
            int[] S_intermediate = fisherYatesShuffle(fisherYatesShuffle(S0.clone(), h0), h1);

            // === STAGE 2: Keyed RNG-Driven Final Permutation ===
            int[] S_final = finalKeyedPermutation(S_intermediate, k);

            double nl = calculateNonlinearity(S_final);
            double sac = calculateSAC(S_final, 1000);

            long endTime = System.nanoTime();
            double execTimeMs = (endTime - startTime) / 1_000_000.0;

            boolean bijective = isBijective(S_final);

            System.out.printf("Run %d: NL=%3d | SAC=%.4f | Time=%6.2f ms | Bijective=%b | Best D Entropy=%.4f%n",
                    run + 1, (int) nl, sac, execTimeMs, bijective, bestEntropy);

            sumNL += nl;
            sumSAC += sac;
            sumTime += execTimeMs;
        }

        System.out.println("\n" + "=".repeat(95));
        System.out.println("=== VARIAN 4 AVERAGE - Stage 2 Keyed RNG-Driven Final Permutation ===");
        System.out.printf("NL          : %.1f%n", sumNL / runs);
        System.out.printf("SAC         : %.4f%n", sumSAC / runs);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / runs);
        System.out.println("=".repeat(95));
    }

    // ==================== HELPER METHODS (identik dengan Varian 3 Revised Final) ====================
    private static ECPoint nextPoint(ECPoint P, ECNamedCurveParameterSpec spec, BigInteger p) {
        BigInteger y = P.getYCoord().toBigInteger();
        if (y.compareTo(p.divide(BigInteger.valueOf(2))) < 0) return P.negate();
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

    // STAGE 1 REVISED: Keyed Byte-Mixing (h0 x-dominant, h1 y-dominant)
    private static List<Integer> keyedByteMixing(List<ECPoint> points, BigInteger k, boolean isH0) {
        List<Integer> mixed = new ArrayList<>();
        byte[] keyBytes = k.toByteArray();
        for (int idx = 0; idx < points.size(); idx++) {
            ECPoint pt = points.get(idx);
            byte[] xBytes = pt.getXCoord().toBigInteger().toByteArray();
            byte[] yBytes = pt.getYCoord().toBigInteger().toByteArray();

            int startX = Math.max(0, xBytes.length - 16);
            int startY = Math.max(0, yBytes.length - 16);

            int dominantStart = isH0 ? startX : startY;
            int supportStart  = isH0 ? startY : startX;
            byte[] dominantBytes = isH0 ? xBytes : yBytes;
            byte[] supportBytes  = isH0 ? yBytes : xBytes;

            for (int i = 0; i < 8; i++) {
                int bDom = (int) dominantBytes[dominantStart + i] & 0xFF;
                int bSup = (int) supportBytes[supportStart + (i % 4)] & 0xFF;
                int m = bDom ^ bSup;
                int rot = keyBytes[idx % keyBytes.length] & 0x07;
                m = Integer.rotateLeft(m, rot);
                int offset = (idx % 256) + (keyBytes[(idx + 11) % keyBytes.length] & 0xFF);
                m = (m + offset) & 0xFF;
                mixed.add(m);
            }
            for (int i = 0; i < 4; i++) {
                int bSup = (int) supportBytes[supportStart + i] & 0xFF;
                mixed.add(bSup);
            }
        }
        return mixed;
    }

    // STAGE 2: Keyed RNG-Driven Final Permutation
    private static int[] finalKeyedPermutation(int[] sbox, BigInteger k) throws Exception {
        // SHA-256 ONLY for seed derivation
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] seed = md.digest(k.toByteArray());

        // Deterministic keyed random byte stream
        Random rng = new Random(new BigInteger(seed).longValue());

        int[] s = sbox.clone();
        for (int i = s.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int temp = s[i];
            s[i] = s[j];
            s[j] = temp;
        }
        return s;
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