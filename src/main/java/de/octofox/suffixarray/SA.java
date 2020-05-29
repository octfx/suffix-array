package de.octofox.suffixarray;

import java.util.*;

public class SA {
    public void DO(final String text) {
        final Map<Character, List<Integer>> buckets = new TreeMap<>();
        final boolean[] BH = new boolean[text.length()];
        final boolean[] B2H = new boolean[text.length()];
        final int[] count = new int[text.length()];
        final Map<Character, Map<Integer, Integer>> sufinv = new TreeMap<>();


        for (int i = 0; i < text.length(); i++) {
            final Character c = text.charAt(i);
            if (!buckets.containsKey(c)) {
                buckets.put(c, new ArrayList<>());
            }

            final List<Integer> bucket = buckets.get(c);
            bucket.add(i);
            Collections.sort(bucket);

            count[i] = 0;
            BH[i] = false;
            B2H[i] = false;
        }

        buckets.forEach((key, integers) -> {
            System.out.println("\nBucket " + key);
            if (!sufinv.containsKey(key)) {
                sufinv.put(key, new HashMap<>());
            }

            final Map<Integer, Integer> inverse = sufinv.get(key);

            for (int i = 0; i < integers.size(); i++) {
                inverse.put(integers.get(i), i);
                System.out.println(integers.get(i) + "=" + text.substring(integers.get(i)) + "| inv = " + inverse.get(i));
            }
        });
    }
}
