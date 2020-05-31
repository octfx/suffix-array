package de.octofox.suffixarray;

import java.util.Map;
import java.util.TreeMap;

public class SuffixArray {
    private final String text;
    private final char[] textChars;
    private final int textLength;

    /**
     * Holds the suffix at a given rank
     * Sorted lexicographically
     * Index equals lexicographic rank, value equals index of suffix
     * Manber Myers: POS
     */
    private final int[] suffixAtRank;

    /**
     * Holds the rank of a given suffix position
     * E.g.: rankOfSuffix[ SuffixPos ] = Rank
     * Index equals index of suffix, value equals lexicographic rank
     * Manber Myers: PRM
     */
    private final int[] rankOfSuffix;

    /**
     * Value is true if the suffix is the lexicographically smallest in its bucket, false otherwise
     * Manber Myers: BH
     */
    private final boolean[] smallestSuffixInBucket;

    /**
     * TODO
     */
    private final boolean[] B2H;

    private int bucketCount;
    /**
     * Internal array
     * Maps bucket => size
     */
    private final int[] intervals;

    /**
     * Array holding the offsets for each bucket
     * Every index with smallestSuffixInBucket[ index ] == true has an entry in 'offsetInBucket'
     * Used in the 2h stage for placing the next sorted suffix in a bucket
     * Manber Myers: Count
     */
    private final int[] nextFreeIndexInBucket;

    public SuffixArray(final String text) {
        this.text = text;
        this.textChars = text.toCharArray();
        this.textLength = text.length();

        suffixAtRank = new int[textLength];
        rankOfSuffix = new int[textLength];
        smallestSuffixInBucket = new boolean[textLength];
        B2H = new boolean[textLength];
        nextFreeIndexInBucket = new int[textLength];

        intervals = new int[textLength];

        final long start = System.nanoTime();

        firstStageSort();
        final long endFirstStageSort = System.nanoTime();

        hStageSort();
        final long endhStageSort = System.nanoTime();

        System.out.println("First stage sort took " + ((endFirstStageSort - start) / 1000000) + "ms");
        System.out.println("h stage sort took " + ((endhStageSort - endFirstStageSort) / 1000000) + "ms");
        System.out.println("i | Bh | sufinv | suftab | suffix");
        for (int i = 0; i < textLength; i++) {
            System.out.println(i + " | " + (smallestSuffixInBucket[i] ? 1 : 0) + "  | " + rankOfSuffix[i] + "      | " + suffixAtRank[i] + "      | " + text.substring(suffixAtRank[i]));
        }
    }

    private void firstStageSort() {
        // Mapping from Character to bucket size
        // TreeMap implementation with explicit Char comparator guarantees lexicographical sort order
        Map<Character, Integer> alphabet = new TreeMap<>(Character::compareTo);

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
            suffixAtRank[i] = -1;
            smallestSuffixInBucket[i] = false;
            //B2H[t] = false;
            nextFreeIndexInBucket[i] = 0;
        }

        int currentSuffix = 0;

        // First stage sort comparing the first character of each suffix
        for (char x : textChars) {
            // Start position of a bucket in the suffix array
            int positionOfStartOfBucket = 0;
            // First free position in a bucket
            int firstFreeIndexInBucket;

            // For each character of the text iterate over the alphabet
            // This requires a lexicographically sorted alphabet as the alphabet order defines the bucket offsets
            // => The first character in the alphabet is the first bucket
            for (Map.Entry<Character, Integer> entry : alphabet.entrySet()) {
                // If text character does not match the current alphabet character:
                // add the size of the characters bucket to the current offset
                // Character bucket 'a' will typically start at offset 0
                // Character bucket 'b' will start at 0 + size of bucket for 'a'
                // Character bucket 'c' will start at 0 + size of bucket for 'a' + size of bucket for 'b'
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
            firstFreeIndexInBucket = positionOfStartOfBucket;

            // Mark the position as the start of the bucket
            smallestSuffixInBucket[positionOfStartOfBucket] = true;

            // Check if the current index is already in use
            // If so increment and check the next index
            while (suffixAtRank[firstFreeIndexInBucket] != -1) {
                firstFreeIndexInBucket++;
            }

            // Set the suffix index at the current rank
            // POS array in Manber Myers Algorithm
            // Mapping from Rank => Suffix = Rank_i has Suffix X
            suffixAtRank[firstFreeIndexInBucket] = currentSuffix;

            // Set rank of the current suffix
            // PRM array in Manber Myers
            // Mapping from Suffix to Rank => Suffix_i has Rank of X
            rankOfSuffix[currentSuffix] = firstFreeIndexInBucket;
            currentSuffix++;
        }
    }

    private void hStageSort() {
        for (int h = 1; h < textLength; h *= 2) {
            resetRankOfSuffixArray();

            int d = textLength - h;
            int e = rankOfSuffix[d];
            rankOfSuffix[d] = e + nextFreeIndexInBucket[e];
            nextFreeIndexInBucket[e]++;
            B2H[rankOfSuffix[d]] = true;

            // For each bucket (interval)
            for (int i = 0; i < textLength; i = intervals[i]) {
                // For each element in the bucket
                for (int j = i; j < intervals[i]; j++) {
                    if (suffixAtRank[j] - h >= 0) {
                        e = rankOfSuffix[j];
                        rankOfSuffix[j] = e + nextFreeIndexInBucket[e];
                        nextFreeIndexInBucket[e]++;
                        B2H[rankOfSuffix[j]] = true;
                    }
                }

                for (int j = i; j < intervals[i]; j++) {
                    d = suffixAtRank[j] - h;
                    if ( d >= 0 && B2H[rankOfSuffix[d]] ) {
                        for (int jInner = rankOfSuffix[d] + 1; jInner < textLength; jInner++) {
                            if (smallestSuffixInBucket[jInner] || !B2H[jInner]) {
                                break;
                            }

                        }
                    }
                }
            }


        }

/*
        for (int h = 1; h < textLengthh; h *= 2) {

            nextFreeIndexInBucket[rankOfSuffix[textLength - h]]++;
            B2H[rankOfSuffix[textLength - h]] = true;

            for (int i=0; i<textLength; i=next[i])   //Scan all buckets and update PRM, Count and B2H arrays
            {
                for (int j=i; j<next[i]; j++)           //Update arrays
                {
                    int s = suffixRank[j] -h;
                    if (s>=0)
                    {
                        int tmp = rankOfSuffix[s];
                        rankOfSuffix[s] = tmp + nextFreeIndexInBucket[tmp]++;
                        B2H[rankOfSuffix[s]] = true;
                    }
                }
                for (int j=i; j<next[i]; j++)           //Reset B2H array such that only the leftmost of them in each
                {                                       //2H-bucket is set to 1, and rest are reset to 0
                    int s = suffixRank[j] - h;
                    if (s>=0 && B2H[rankOfSuffix[s]])
                    {
                        for (int k=rankOfSuffix[s] + 1; k<FindNextBH(k, BH, textLength); k++)
                            B2H[k]=false;
                    }
                }
            }

            for (int i=0; i<textLength; i++)         //Updating POS and BH arrays
            {
                suffixRank[rankOfSuffix[i]]=i;
                smallestSuffixInBucket[i] |= B2H[i];
            }
        }*/
    }


    /**
     * Reset rankOfSuffix (PRM) array to point to the leftmost cell of the H-bucket containing the ith suffix
     * First compute the needed intervals
     * Intervals are in the form of Bucket start => next bucket start
     * First Bucket (index 0) holds the value for the next bucket start
     */
    private void resetRankOfSuffixArray() {
        bucketCount = 0;
        int sizeOfBucket;

        // Computation of intervals
        for (int i = 0; i < textLength; i = sizeOfBucket) {
            sizeOfBucket = 1 + i;

            // Loops from bucket start to bucket end
            while (sizeOfBucket < textLength && !smallestSuffixInBucket[sizeOfBucket]) {
                sizeOfBucket++;
            }

            // Map interval Start to interval end
            intervals[i] = sizeOfBucket;
            bucketCount++;
        }

        // For each interval
        // i = start of bucket (left interval boundary)
        for (int i = 0; i < textLength; i = intervals[i]) {
            nextFreeIndexInBucket[i] = 0;

            // assign left boundary (i) to the suffix in bucket
            // j = right interval boundary
            for (int j = i; j < intervals[i]; j++) {
                rankOfSuffix[suffixAtRank[i]] = i;
            }
        }
    }
}
