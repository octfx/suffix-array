package de.octofox.suffixarray.search;

import de.octofox.suffixarray.SuffixArray;

/**
 * A naive binary search
 */
public class NaiveSearch implements Search {
    final SuffixArray sa;
    final String text;

    public NaiveSearch(final SuffixArray suffixArray, final String text) {
        this.sa = suffixArray;
        this.text = text;
    }

    public void search(final String query) {
        final int textLength = text.length();

        int l = 0;
        int r = textLength - 1;

        final long searchStart = System.nanoTime();

        while (r - l > 1) {
            int mid = (l + r) / 2;

            String substringText = text.substring(sa.suffixAtRank(mid), Math.min(sa.suffixAtRank(mid) + query.length(), textLength));

            int res = query.compareTo(substringText);

            if (res == 0) {
                String out = text.substring(Math.max(0, sa.suffixAtRank(mid) - 10 - substringText.length()), Math.min(sa.suffixAtRank(mid) + 10 + substringText.length(), textLength));

                out = out.replaceFirst(query, ">" + query + "<");

                final long searchEnd = System.nanoTime() - searchStart;

                System.out.println("Found pattern '" + query + "' at index " + sa.suffixAtRank(mid) + " (" + out.replace("\n", " ").replace("\r", " ") + ")");
                System.out.println("Search took " + searchEnd / 1000000 + " ms\n");

                return;
            }

            if (res < 0) {
                r = mid;
            } else {
                l = mid;
            }
        }

        System.out.println("Pattern '" + query + "' not found.");
    }
}
