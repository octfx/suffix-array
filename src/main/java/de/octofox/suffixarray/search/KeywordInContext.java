package de.octofox.suffixarray.search;

import de.octofox.suffixarray.SuffixArray;

/**
 * Keyword in Context search from
 * https://algs4.cs.princeton.edu/63suffix/KWIK.java.html
 */
public class KeywordInContext implements Search {
    final SuffixArray sa;
    final String text;

    public KeywordInContext(final SuffixArray suffixArray, final String text) {
        this.sa = suffixArray;
        this.text = text;
    }

    public void search(final String query) {
        final int n = text.length();
        final int context = 15;

        System.out.println("\n");

        for (int i = sa.rank(query); i < n; i++) {
            int from1 = sa.suffixAtRank(i);
            int to1 = Math.min(n, from1 + query.length());
            if (!query.equals(text.substring(from1, to1))) {
                break;
            }
            int from2 = Math.max(0, sa.suffixAtRank(i) - context);
            int to2 = Math.min(n, sa.suffixAtRank(i) + context + query.length());
            System.out.println(text.substring(from2, to2).replace("\n", " ").replace("\r", " "));
        }

        System.out.println("\n");
    }
}
