package de.octofox.suffixarray;

import java.util.Arrays;

public class SuffixArray {
    private final String text;

    private Entry[] L;

    /**
     * P contains all sorted buckets after H stages
     * Entries represent Buckets in each H step
     * Each Step should be equal to the Pos array described in page 5 (first line)
     * "At the start of stage H, Pos [i] contains the start position of the irh smallest suffix"
     */
    private int[][] P;

    /**
     * Length of the text
     */
    private int N;

    private class Entry {
        private int[] nr;
        private int p;

        public Entry(int[] nr, int p) {
            this.nr = nr;
            this.p = p;
        }
    }

    public SuffixArray(final String text) {
        this.text = text;

        this.N = text.length();

        this.L = new Entry[N];

        // log steps X text length
        // The sorting is done in log2(N+1) stages
        // Paper Page 4. Chapter 3. Sorting. First line
        this.P = new int[(int) Math.log((N + 1))][N];

        // TODO not tested
        this.sort();
    }

    /**
     * Compares two entries
     *
     * @param a First Entry
     * @param b Second Entry
     * @return 1 if a is smaller than b
     */
    private int cmp(Entry a, Entry b) {
        return a.nr[0] == b.nr[0] ? (a.nr[1] < b.nr[1] ? 1 : 0) : (a.nr[0] < b.nr[0] ? 1 : 0);
    }

    private void sort() {
        int stp;
        int cnt;

        // First Stage
        // The first stage consists of a bucket sort according to the first symbol of each suffix.
        // Paper Page 4. Chapter 3. Sorting
        for (int i = 0; i < N; i++) {
            P[0][i] = this.text.charAt(i) - 'a';
        }

        // Further Sorting Stages
        // Each stage doubles the length of compared suffixes
        for (stp = 1, cnt = 1; cnt >> 1 < N; stp++, cnt <<= 1) {
            // TODO
            for (int i = 0; i < N; i++) {
                L[i].nr[0] = P[stp - 1][i];
                L[i].nr[1] = (i + cnt < N) ? P[stp - 1][i + cnt] : -1;
                L[i].p = i;
            }

            Arrays.sort(L, this::cmp);

            // TODO
            for (int i = 0; i < N; i++) {
                P[stp][L[i].p] =
                        i > 0 &&
                                L[i].nr[0] == L[i - 1].nr[0] &&
                                L[i].nr[1] == L[i - 1].nr[1] ? P[stp][L[i - 1].p] : i;
            }
        }
    }
}
