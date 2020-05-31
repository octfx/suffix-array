package de.octofox.suffixarray;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Manber Myer implementation of suffix arrays
 */
public class SuffixArray {
    /**
     * The input text
     */
    private final String text;

    /**
     * Char array of the input text
     */
    private final char[] textChars;

    /**
     * Length of the input text
     */
    private final int textLength;

    /**
     * Alphabet of the input text
     * Mapping from Character to bucket size
     */
    private final Map<Character, Integer> alphabet;

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
     * Marks the start of a bucket
     * Manber Myers: BH
     */
    private final boolean[] smallestSuffixInBucket;

    /**
     * Value is true if the suffix is the lexicographically smallest in its 2h bucket, false otherwise
     * Manber Myers: B2H
     */
    private final boolean[] smallestSuffixIn2hBucket;

    /**
     * Array holding the offsets for each bucket
     * Every index with smallestSuffixInBucket[ index ] == true has an entry in 'offsetInBucket'
     * Used in the 2h stage for placing the next sorted suffix in a bucket
     * Manber Myers: Count
     */
    private final int[] nextFreeIndexInBucket;

    /**
     * Internal counter for buckets
     * Used to exit 2h stage early
     * If bucketCount == textLength => SArray is sorted according to 2h stage
     */
    private int bucketCount;

    /**
     * Internal array
     * "Linked list" maps value of index 0 to start of next bucket (index)
     * Start of next bucket links to its next bucket
     * E.g.: index 0 => value is next index in array
     */
    private final int[] intervals;

    /**
     * Suffix array computation using the manber myers algorithm
     *
     * @param text The text to generate the suffix array for
     */
    public SuffixArray(final String text) {
        this.text = text;
        this.textChars = text.toCharArray();
        this.textLength = text.length();

        // TreeMap implementation with explicit Char comparator guarantees lexicographic sorting order
        this.alphabet = new TreeMap<>(Character::compareTo);

        this.suffixAtRank = new int[textLength];
        this.rankOfSuffix = new int[textLength];
        this.smallestSuffixInBucket = new boolean[textLength];
        this.smallestSuffixIn2hBucket = new boolean[textLength];
        this.nextFreeIndexInBucket = new int[textLength];

        this.intervals = new int[textLength];
    }

    /**
     * Entry method
     * Computes the alphabet, does the first- and h-stage sort
     */
    public void compute() {
        final long start = System.nanoTime();

        computeAlphabet();
        final long endComputeAlphabet = System.nanoTime();

        firstStageSort();
        final long endFirstStageSort = System.nanoTime();

        hStageSort();
        final long endhStageSort = System.nanoTime();

        final long end = System.nanoTime();

        final long timeAlphabet = endComputeAlphabet - start;
        final long timeFirstStage = endFirstStageSort - endComputeAlphabet;
        final long timeHStage = endhStageSort - endFirstStageSort;
        final long timeElapsedTotal = end - start;

        Runtime runtime = Runtime.getRuntime();

        try                                             //Saving Suffix array to file
        {
            write();
        } catch (Exception e) {
            System.out.println("Something went wrong during saving SA to file. Check if you are Administrator user on this PC maybe.");
        }

        System.out.println("Computing the alphabet took " + (timeAlphabet / 1000000) + "ms");
        System.out.println("First stage sort took " + (timeFirstStage / 1000000) + "ms");
        System.out.println("h stage sort took " + (timeHStage / 1000000) + "ms");

        System.out.println("\n\nSA created in " + timeElapsedTotal / 1000000 + " ms");
        System.out.println("Used memory " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB");

    }

    /**
     * Computes the bucket size for each character
     */
    private void computeAlphabet() {
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
            smallestSuffixIn2hBucket[i] = false;
            nextFreeIndexInBucket[i] = 0;
        }
    }

    /**
     * First stage sort
     * Sorts all suffixes according to their first character
     * Sets suffixAtRank, rankOfSuffix and smallestSuffixInBucket
     */
    private void firstStageSort() {
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

    private int rightBucketBoundary(int x) {
        for (int i = x; i < textLength; i++) {
            if (smallestSuffixInBucket[i])
                return i;
        }
        return textLength;
    }

    private void write() throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter("SAOutput.txt"))) {
            out.write("Sorted suffixes:\n");
            out.write("[\n");
            for (int x : suffixAtRank) {
                out.write(x + ": " + text.substring(x) + ", \n");
            }
            out.write("]\n\n\nRank of Suffix (inverse SA)\n[");

            out.write(Arrays.stream(rankOfSuffix).mapToObj(String::valueOf).reduce("", (left, right) -> left + ", " + right));

            out.write("]");
        }
    }

    private void hStageSort() {
        for (int h = 1; h < textLength; h *= 2) {
            resetRankOfSuffixArray();

            if (bucketCount == textLength) {
                return;
            }

            // d <- N-H
            int d = textLength - h;
            // e <- Prm[d]
            int e = rankOfSuffix[d];
            // Prm[d] <- e + Count[e]
            rankOfSuffix[d] = e + nextFreeIndexInBucket[e];
            // Count[e] <- Count[e] + 1
            nextFreeIndexInBucket[e]++;
            // B2H[Prm[d]] <- true
            smallestSuffixIn2hBucket[rankOfSuffix[d]] = true;

            // For each bucket (interval)
            for (int i = 0; i < textLength; i = intervals[i]) {
                // For each element in the bucket
                // for d \in {Pos[c] - H : c \in [l,r]} \cap [0, N-1] do
                for (d = i; d < intervals[i]; d++) {
                    final int s = suffixAtRank[d] - h;
                    if (s >= 0) {
                        // e <- Prm[d]
                        e = rankOfSuffix[s];
                        // Prm[d] <- e + Count[e]
                        rankOfSuffix[s] = e + nextFreeIndexInBucket[e];
                        // Count[e] <- Count[e] + 1
                        nextFreeIndexInBucket[e]++;
                        // B2H[Prm[d]] <- true
                        smallestSuffixIn2hBucket[rankOfSuffix[s]] = true;
                    }
                }

                // for d \in {Pos[c] - H : c \in [l,r]} \cap [O,N-1] do
                for (d = i; d < intervals[i]; d++) {
                    final int s = suffixAtRank[d] - h;
                    // if B2H[Prm[d]] then
                    if (s >= 0 && smallestSuffixIn2hBucket[rankOfSuffix[s]]) {

                        for (int k = rankOfSuffix[s] + 1; k < rightBucketBoundary(k); k++) {
                            smallestSuffixIn2hBucket[k] = false;
                        }
                    }
                }
            }

            //Updating POS and BH arrays
            for (int i = 0; i < textLength; i++) {
                suffixAtRank[rankOfSuffix[i]] = i;

                if (smallestSuffixIn2hBucket[i] && !smallestSuffixInBucket[i]) {
                    //Set(i, H + Min_Height(Prm[Pos[i 1] + HI, Prm[Pos[i] + H]))
                    smallestSuffixInBucket[i] = smallestSuffixIn2hBucket[i];
                }
            }
        }
    }

    /**
     * Reset rankOfSuffix (PRM) array to point to the leftmost cell of the H-bucket containing the ith suffix
     * First compute the needed intervals
     * Intervals are in the form of Bucket start => next bucket start
     * First Bucket (index 0) holds the value for the next bucket start
     * Then assign the left boundary of the interval to the corresponding bucket entries
     */
    private void resetRankOfSuffixArray() {
        bucketCount = 0;
        int sizeOfBucket;

        // Computation of intervals
        for (int i = 0; i < textLength; i = sizeOfBucket) {
            // Offset by one so that smallestSuffixInBucket is not immediately true
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
                rankOfSuffix[suffixAtRank[j]] = i;
            }
        }
    }
}
