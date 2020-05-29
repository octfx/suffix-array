package de.octofox.suffixarray;

import java.util.*;

public class SuffixArray {
    private final String text;
    private final char[] textChars;

    /**
     * Holds the suffix at a given position
     * Sorted lexicographically
     */
    private final int[] suffixRank;

    /**
     * Holds the rank of a given suffix position
     * E.g.: rankOfSuffix[ SuffixPos ] = Rank
     */
    private final int[] rankOfSuffix;

    /**
     * Value is true if the suffix is the lexicographically smallest in its bucket, false otherwise
     */
    private final boolean[] smallestSuffixInBucket;


    public SuffixArray(final String text) {
        this.text = text;
        this.textChars = text.toCharArray();

        suffixRank = new int[text.length()];
        rankOfSuffix = new int[text.length()];
        smallestSuffixInBucket = new boolean[text.length()];

        firstStageSort();

        System.out.println("i | Bh | sufinv | suftab | suffix");
        for (int i = 0; i < text.length(); i++) {
            System.out.println(i + " | " + (smallestSuffixInBucket[i] ? 1 : 0)  + "  | " + rankOfSuffix[i] + "      | " + suffixRank[i] + "      | " + text.substring(suffixRank[i]));
        }
    }

    private void firstStageSort() {
        // Mapping from Character to bucket size
        Map<Character, Integer> alphabet = new HashMap<>();

        // First iterate over all characters of the text
        for (int i = 0; i < textChars.length; i++) {
            final char currentChar = textChars[i];

            // If alphabet contains the current character increment the size of the bucket
            // E.g.: input 'abaccc'
            // => Bucket size for char 'a' = 2
            // => Bucket size for char 'b' = 1
            // => Bucket size for char 'c' = 3
            if (alphabet.containsKey(currentChar)) {
                int bucketSize = alphabet.get(currentChar);

                bucketSize++;

                alphabet.put(currentChar, bucketSize);
            } else {
                // Else set bucket size to 1
                alphabet.put(currentChar, 1);
            }

            // Base value initialization
            suffixRank[i] = -1;
            smallestSuffixInBucket[i] = false;
            //B2H[t] = false;
            //Count[t] = 0;

        }

        int currentSuffix = 0;

        // First stage sort comparing the first character of each suffix
        for (char x : textChars) {
            // Start position of a bucket in the suffix array
            int positionOfStartOfBucket = 0;
            // First free position in a bucket
            int offset;

            // For each character of the text iterate over the alphabet
            for (Map.Entry<Character, Integer> entry: alphabet.entrySet()) {
                // If text character does not match the current alphabet character:
                // add the size of the characters bucket to the current offset
                // Character 'a' will typically start at offset 0
                // Character 'b' will start at 0 + size of bucket for 'a'
                // Character 'c' will start at 0 + size of bucket for 'a' + size of bucket for 'b'
                // ...
                if (x != entry.getKey()) {
                    int bucketSizeOffset = entry.getValue();
                    positionOfStartOfBucket += bucketSizeOffset;
                } else {
                    // Characters match
                    // first position of corresponding bucket has been computed
                    break;
                }
            }

            // positionOfStartOfBucket will always be first position of bucket
            offset = positionOfStartOfBucket;

            // Mark the position as the start of the bucket
            smallestSuffixInBucket[positionOfStartOfBucket] = true;

            // Check if the current index is already in use
            // If so increment offset by one
            while (suffixRank[offset] != -1) {
                offset++;
            }

            // Set the rank of the current suffix
            // POS array in Manber Myers Algorithm
            // Mapping from Rank => Suffix => Suffix_i has the lexicographic rank of X
            suffixRank[offset] = currentSuffix;

            // Set the inverse array
            // PRM array in Manber Myers
            // Mapping from Suffix to Rank => Suffix_i has Rank of X
            rankOfSuffix[currentSuffix] = offset;
            currentSuffix++;
        }
    }

}
