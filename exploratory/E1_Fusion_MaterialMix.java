// Exploratory / pra-final design — label E1 (Lampiran A).
// Bukan rancangan official Bab 4. Official: src/Varian1..Varian4_*.java
// Legacy source class name: E1_Fusion_MaterialMix
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class E1_Fusion_MaterialMix {

    public static void main(String[] args) throws Exception {
        String keyHex = "0123456789abcdef0123456789abcdef";
        int L = 4096;
        int runs = 50;                 // pilot run dulu
        int numDCandidates = 8;

        System.out.println("VARIAN 3_FUSION - Stage 1 Final Fusion");
        System.out.println("L = " + L + " | D Candidates = " + numDCandidates + " | Runs = " + runs);
        System.out.println("=".repeat(95));

        double sumNL = 0.0;
        double sumSAC = 0.0;
        double sumTime = 0.0;

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        BigInteger p = spec.getCurve().getField().getCharacteristic();
        BigInteger order = spec.getN();
        ECPoint G = spec.getG();
        BigInteger k = new BigInteger(keyHex, 16).mod(order);
        byte[] keyBytes = k.toByteArray();

        SecureRandom sr = new SecureRandom();

        for (int run = 0; run < runs; run++) {
            long startTime = System.nanoTime();

            List<ECPoint> points = new ArrayList<>();
            ECPoint P = G;
            for (int i = 0; i < L; i++) {
                points.add(P);
                P = nextPoint(P, spec, p);
            }

            ECPoint bestD = null;
            double bestEntropy = -1.0;

            for (int cand = 0; cand < numDCandidates; cand++) {
                BigInteger scalar = new BigInteger(order.bitLength(), sr)
                        .mod(order.subtract(BigInteger.ONE))
                        .add(BigInteger.ONE);   // force 1 .. order-1

                ECPoint candidateD = G.multiply(scalar).normalize();
                List<ECPoint> rPoints = generateRPoints(points, candidateD);
                double entropy = calculateEntropy(rPoints);

                if (entropy > bestEntropy) {
                    bestEntropy = entropy;
                    bestD = candidateD;
                }
            }

            List<ECPoint> qPoints = new ArrayList<>();
            ECPoint k2G = G.multiply(k.multiply(BigInteger.valueOf(2)));
            for (ECPoint pt : points) {
                qPoints.add(pt.add(k2G));
            }

            List<ECPoint> rPoints = generateRPoints(points, bestD);

            List<Integer> rngStreamH0 = generateRNGStream(k, "EXT_H0", qPoints.size() * 3);
            List<Integer> rngStreamH1 = generateRNGStream(k, "EXT_H1", rPoints.size() * 3);

            List<Integer> h0 = buildControlledMaterial(qPoints, rngStreamH0, true, keyBytes);
            List<Integer> h1 = buildControlledMaterial(rPoints, rngStreamH1, false, keyBytes);

            int[] s0 = new int[256];
            for (int i = 0; i < 256; i++) {
                s0[i] = i;
            }

            int[] s1 = fisherYatesShuffle(s0, h0);
            int[] s2 = fisherYatesShuffle(s1, h1);

            double nl = calculateNonlinearity(s2);
            double sac = calculateSAC(s2, 1000, 0xABCDEF1234567890L + run);
            double execTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
            boolean bijective = isBijective(s2);

            System.out.printf(
                    "Run %d: NL=%3d | SAC=%.4f | Time=%7.2f ms | Bijective=%b | Best D Entropy=%.4f%n",
                    run + 1, (int) nl, sac, execTimeMs, bijective, bestEntropy
            );

            sumNL += nl;
            sumSAC += sac;
            sumTime += execTimeMs;
        }

        System.out.println("\n" + "=".repeat(95));
        System.out.println("=== VARIAN 3_FUSION AVERAGE - Stage 1 Final Fusion ===");
        System.out.printf("NL          : %.1f%n", sumNL / runs);
        System.out.printf("SAC         : %.4f%n", sumSAC / runs);
        System.out.printf("Waktu       : %.2f ms%n", sumTime / runs);
        System.out.println("=".repeat(95));
    }

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
            BigInteger legendre = rhs.modPow(
                    p.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)), p
            );

            if (legendre.equals(BigInteger.ONE)) {
                BigInteger yNew = rhs.modPow(
                        p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)), p
                );
                return spec.getCurve().createPoint(x, yNew);
            }
            x = x.add(BigInteger.ONE);
        }
    }

    private static List<ECPoint> generateRPoints(List<ECPoint> points, ECPoint D) {
        List<ECPoint> r = new ArrayList<>();
        for (ECPoint q : points) {
            r.add(q.add(D));
        }
        return r;
    }

    private static byte[] normalizeCoordToFixed32(ECPoint pt, boolean isX) {
        BigInteger coord = isX
                ? pt.getXCoord().toBigInteger()
                : pt.getYCoord().toBigInteger();

        byte[] raw = coord.toByteArray();
        byte[] fixed = new byte[32];

        int copyLen = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copyLen, fixed, 32 - copyLen, copyLen);
        return fixed;
    }

    private static List<Integer> generateRNGStream(BigInteger k, String domain, int requiredLength) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] seed = md.digest(k.toByteArray());

        List<Integer> stream = new ArrayList<>(requiredLength);
        byte[] current = seed;
        long counter = 0L;
        byte[] domainBytes = domain.getBytes(StandardCharsets.UTF_8);

        while (stream.size() < requiredLength) {
            byte[] input = new byte[current.length + domainBytes.length + 8];
            System.arraycopy(current, 0, input, 0, current.length);
            System.arraycopy(domainBytes, 0, input, current.length, domainBytes.length);

            for (int i = 0; i < 8; i++) {
                input[current.length + domainBytes.length + i] =
                        (byte) (counter >>> (56 - i * 8));
            }

            current = md.digest(input);

            for (byte b : current) {
                stream.add(b & 0xFF);
                if (stream.size() >= requiredLength) {
                    break;
                }
            }
            counter++;
        }

        return stream;
    }

    private static List<Integer> buildControlledMaterial(
            List<ECPoint> points,
            List<Integer> rngStream,
            boolean isH0,
            byte[] keyBytes
    ) {
        List<Integer> material = new ArrayList<>(points.size() * 12);
        int rngIdx = 0;
        int ptIdx = 0;

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

            byte[] mixedDom = new byte[8];
            for (int i = 0; i < 8; i++) {
                int d = domBytes[i] & 0xFF;
                int s = supBytes[i % 4] & 0xFF;
                int keyByte = keyBytes[(ptIdx + i) % keyBytes.length] & 0xFF;

                int m = d ^ s;
                m = rol8(m, keyByte & 0x07);
                m = (m + ((ptIdx + i) & 0xFF)) & 0xFF;

                mixedDom[i] = (byte) m;
            }

            byte[] result = new byte[12];

            if (interleaveMode == 0) {
                for (int i = 0; i < 4; i++) {
                    result[2 * i] = mixedDom[i];
                    result[2 * i + 1] = supBytes[i];
                }
                System.arraycopy(mixedDom, 4, result, 8, 4);

            } else if (interleaveMode == 1) {
                for (int i = 0; i < 4; i++) {
                    result[2 * i] = supBytes[i];
                    result[2 * i + 1] = mixedDom[i];
                }
                System.arraycopy(mixedDom, 4, result, 8, 4);

            } else if (interleaveMode == 2) {
                System.arraycopy(mixedDom, 0, result, 0, 2);
                System.arraycopy(supBytes, 0, result, 2, 2);
                System.arraycopy(mixedDom, 2, result, 4, 2);
                System.arraycopy(supBytes, 2, result, 6, 2);
                System.arraycopy(mixedDom, 4, result, 8, 4);

            } else {
                System.arraycopy(supBytes, 0, result, 0, 2);
                System.arraycopy(mixedDom, 0, result, 2, 2);
                System.arraycopy(supBytes, 2, result, 4, 2);
                System.arraycopy(mixedDom, 2, result, 6, 2);
                System.arraycopy(mixedDom, 4, result, 8, 4);
            }

            for (byte b : result) {
                material.add(b & 0xFF);
            }

            ptIdx++;
        }

        return material;
    }

    private static int rol8(int value, int shift) {
        shift &= 7;
        return ((value << shift) | (value >>> (8 - shift))) & 0xFF;
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
            if (nl < minNL) {
                minNL = nl;
            }
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
        for (int v : h) {
            if (Math.abs(v) > max) {
                max = Math.abs(v);
            }
        }
        return max;
    }

    private static double calculateSAC(int[] sbox, int trials, long seed) {
        int changes = 0;
        Random rand = new Random(seed);

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
        for (int v : sbox) {
            set.add(v);
        }
        return set.size() == 256;
    }

    private static double calculateEntropy(List<ECPoint> rPoints) {
        int[] count = new int[256];

        for (ECPoint r : rPoints) {
            byte[] xBytes = r.getXCoord().toBigInteger().toByteArray();
            int val = xBytes[xBytes.length - 1] & 0xFF;
            count[val]++;
        }

        double entropy = 0.0;
        for (int c : count) {
            if (c > 0) {
                double prob = (double) c / rPoints.size();
                entropy -= prob * Math.log(prob) / Math.log(2);
            }
        }
        return entropy;
    }
}