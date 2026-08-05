// Exploratory / pra-final design — label E4 (Lampiran A).
// Bukan rancangan official Bab 4. Official: src/Varian1..Varian4_*.java
// Legacy source class name: E4_LightPerturbation
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

public class E4_LightPerturbation {

    public static void main(String[] args) throws Exception {
        String keyHex = "0123456789abcdef0123456789abcdef";
        int L = 4096;
        int runs = 50;
        int numDCandidates = 8;

        System.out.println("🚀 VARIAN 3_New_v2 – RNG-Controlled Coordinate Extraction Hybrid (Light Perturbation)");
        System.out.println("L = " + L + " | D Candidates = " + numDCandidates + " | Runs = " + runs);
        System.out.println("=".repeat(95));

        double sumNL = 0, sumSAC = 0, sumTime = 0;

        for (int run = 0; run < runs; run++) {
            long startTime = System.nanoTime();

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            BigInteger p = spec.getCurve().getField().getCharacteristic();
            BigInteger order = spec.getN();
            BigInteger k = new BigInteger(keyHex, 16).mod(order);
            ECPoint G = spec.getG();

            List<ECPoint> points = new ArrayList<>();
            ECPoint P = G;
            for (int i = 0; i < L; i++) {
                points.add(P);
                P = nextPoint(P, spec, p);
            }

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

            List<ECPoint> Q_points = new ArrayList<>();
            for (ECPoint pt : points) {
                Q_points.add(pt.add(G.multiply(k.multiply(BigInteger.valueOf(2)))));
            }
            List<ECPoint> R_points = generateRPoints(points, bestD);

            // RNG streams untuk extraction control
            List<Integer> rngStreamH0 = generateRNGStream(k, "EXT_H0", Q_points.size() * 3);
            List<Integer> rngStreamH1 = generateRNGStream(k, "EXT_H1", R_points.size() * 3);

            // RNG streams untuk light perturbation (4 posisi per titik)
            List<Integer> rngStreamPertH0 = generateRNGStream(k, "PERT_H0", Q_points.size() * 4);
            List<Integer> rngStreamPertH1 = generateRNGStream(k, "PERT_H1", R_points.size() * 4);

            List<Integer> h0 = buildControlledMaterial(Q_points, rngStreamH0, rngStreamPertH0, true);
            List<Integer> h1 = buildControlledMaterial(R_points, rngStreamH1, rngStreamPertH1, false);

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
        System.out.println("=== VARIAN 3_New_v2 AVERAGE - RNG-Controlled Coordinate Extraction Hybrid (Light Perturbation) ===");
        System.out.printf("NL          : %.1f%n", sumNL / runs);
        System.out.printf("SAC         : %.4f%n", sumSAC / runs);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / runs);
        System.out.println("=".repeat(95));
    }

    // ==================== HELPER METHODS ====================

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

    private static byte[] normalizeCoordToFixed32(ECPoint pt, boolean isX) {
        BigInteger coord = isX ? pt.getXCoord().toBigInteger() : pt.getYCoord().toBigInteger();
        byte[] raw = coord.toByteArray();
        byte[] fixed = new byte[32];
        int copyLen = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copyLen, fixed, 32 - copyLen, copyLen);
        return fixed;
    }

    private static List<Integer> generateRNGStream(BigInteger k, String domain, int requiredLength) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] seed = md.digest(k.toByteArray());
        List<Integer> stream = new ArrayList<>();
        byte[] current = seed;
        long counter = 0;
        byte[] domainBytes = domain.getBytes();

        while (stream.size() < requiredLength) {
            byte[] input = new byte[current.length + domainBytes.length + 8];
            System.arraycopy(current, 0, input, 0, current.length);
            System.arraycopy(domainBytes, 0, input, current.length, domainBytes.length);
            for (int i = 0; i < 8; i++) {
                input[current.length + domainBytes.length + i] = (byte) (counter >>> (56 - i * 8));
            }
            current = md.digest(input);
            for (byte b : current) {
                stream.add((int) b & 0xFF);
                if (stream.size() >= requiredLength) break;
            }
            counter++;
        }
        return stream;
    }

    private static List<Integer> buildControlledMaterial(List<ECPoint> points, List<Integer> rngStream, List<Integer> pertStream, boolean isH0) {
        List<Integer> material = new ArrayList<>();
        int rngIdx = 0;
        int pertIdx = 0;

        for (ECPoint pt : points) {
            byte[] xFixed = normalizeCoordToFixed32(pt, true);
            byte[] yFixed = normalizeCoordToFixed32(pt, false);

            int offsetX = rngStream.get(rngIdx++) % 16;
            int offsetY = rngStream.get(rngIdx++) % 16;
            int interleaveMode = rngStream.get(rngIdx++) % 4;

            byte[] dominant = isH0 ? xFixed : yFixed;
            byte[] supporting = isH0 ? yFixed : xFixed;
            int domStart = isH0 ? offsetX : offsetY;
            int supStart = isH0 ? offsetY : offsetX;

            byte[] domBytes = new byte[8];
            byte[] supBytes = new byte[4];
            System.arraycopy(dominant, domStart, domBytes, 0, 8);
            System.arraycopy(supporting, supStart, supBytes, 0, 4);

            byte[] result = new byte[12];
            if (interleaveMode == 0) {
                for (int i = 0; i < 4; i++) {
                    result[2 * i] = domBytes[i];
                    result[2 * i + 1] = supBytes[i];
                }
                System.arraycopy(domBytes, 4, result, 8, 4);
            } else if (interleaveMode == 1) {
                for (int i = 0; i < 4; i++) {
                    result[2 * i] = supBytes[i];
                    result[2 * i + 1] = domBytes[i];
                }
                System.arraycopy(domBytes, 4, result, 8, 4);
            } else if (interleaveMode == 2) {
                System.arraycopy(domBytes, 0, result, 0, 2);
                System.arraycopy(supBytes, 0, result, 2, 2);
                System.arraycopy(domBytes, 2, result, 4, 2);
                System.arraycopy(supBytes, 2, result, 6, 2);
                System.arraycopy(domBytes, 4, result, 8, 4);
            } else {
                System.arraycopy(supBytes, 0, result, 0, 2);
                System.arraycopy(domBytes, 0, result, 2, 2);
                System.arraycopy(supBytes, 2, result, 4, 2);
                System.arraycopy(domBytes, 2, result, 6, 2);
                System.arraycopy(domBytes, 4, result, 8, 4);
            }

            // === LIGHT PERTURBATION (v2) – hanya setelah interleave selesai ===
            int[] perturbPositions = {0, 3, 6, 9};
            for (int pos : perturbPositions) {
                int rngPert = pertStream.get(pertIdx++);
                result[pos] = (byte) (result[pos] ^ (rngPert & 0x0F));
            }

            for (byte b : result) {
                material.add((int) b & 0xFF);
            }
        }
        return material;
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
            for (int x = 0; x < 256; x++) f[x] = 1 - 2 * ((sbox[x] >> bit) & 1);
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