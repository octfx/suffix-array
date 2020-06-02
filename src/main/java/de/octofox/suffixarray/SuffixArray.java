package de.octofox.suffixarray;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
     * A naive binary search
     *
     * @param search Substring to search for
     */
    public void naiveSearch(final String search) {
        int l = 0;
        int r = textLength - 1;

        final long searchStart = System.nanoTime();

        while (r - l > 1) {
            int mid = (l + r) / 2;

            String substringText = text.substring(suffixAtRank[mid], Math.min(suffixAtRank[mid] + search.length(), textLength));

            int res = search.compareTo(substringText);

            if (res == 0) {
                String out = text.substring(Math.max(0, suffixAtRank[mid] - 10 - substringText.length()), Math.min(suffixAtRank[mid] + 10 + substringText.length(), textLength));

                out = out.replaceFirst(search, ">" + search + "<");

                final long searchEnd = System.nanoTime() - searchStart;

                System.out.println("Found pattern '" + search + "' at index " + suffixAtRank[mid] + " (" + out + ")");
                System.out.println("Search took " + searchEnd / 1000000 + " ms\n");

                return;
            }

            if (res < 0) {
                r = mid;
            } else {
                l = mid;
            }
        }

        System.out.println("Pattern '" + search + "' not found.");
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

/*        try {
            write();
        } catch (Exception e) {
            //
        }*/

        System.out.println("Computing the alphabet took " + (timeAlphabet / 1000000) + "ms");
        System.out.println("First stage sort took " + (timeFirstStage / 1000000) + "ms");
        System.out.println("h stage sort took " + (timeHStage / 1000000) + "ms");

        System.out.println("\nSA created in " + timeElapsedTotal / 1000000 + " ms");
        System.out.println("Used memory " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB");
    }

    /**
     * Computes the bucket size for each character of the input text
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

    /**
     * h-stage sort
     * Sorting by doubling the size of compared prefixes until text length is reached
     */
    private void hStageSort() {
        for (int h = 1; h < textLength; h *= 2) {
            computeBucketIntervals();
            resetRankOfSuffixArray();

            // d <- N-H
            sort(textLength - h);

            // For each bucket (interval)
            for (int i = 0; i < textLength; i = intervals[i]) {
                // For each element in the bucket
                // for d \in {Pos[c] - H : c \in [l,r]} \cap [0, N-1] do
                for (int d = i; d < intervals[i]; d++) {
                    final int s = suffixAtRank[d] - h;

                    if (s < 0) {
                        continue;
                    }

                    sort(s);
                }

                // for d \in {Pos[c] - H : c \in [l,r]} \cap [O,N-1] do
                for (int d = i; d < intervals[i]; d++) {
                    final int s = suffixAtRank[d] - h;

                    if (s < 0) {
                        continue;
                    }

                    // if B2H[Prm[d]] then
                    if (smallestSuffixIn2hBucket[rankOfSuffix[s]]) {
                        // for f \in [Prm[d] + 1, e-1] do
                        for (int f = rankOfSuffix[s] + 1; f < rightBucketBoundary(f); f++) {
                            // B2H[ f ] <- false
                            smallestSuffixIn2hBucket[f] = false;
                        }
                    }
                }
            }

            // In the final step, we update the Pos array (which is the inverse of Prm), and set BH to B2H.
            for (int i = 0; i < textLength; i++) {
                // Pos[Prm[i]] <- i
                suffixAtRank[rankOfSuffix[i]] = i;

                // if B2H[i] and not BH[i] then
                if (smallestSuffixIn2hBucket[i] && !smallestSuffixInBucket[i]) {
                    //Set(i, H + Min_Height(Prm[Pos[i 1] + HI, Prm[Pos[i] + H]))

                    // BH[i] <- B2H[i]
                    smallestSuffixInBucket[i] = smallestSuffixIn2hBucket[i];
                }
            }
        }
    }

    /**
     * Moves the suffix to the start (+ offset) of its bucket
     *
     * @param index The suffix index to sort
     */
    private void sort(final int index) {
        // e <- Prm[d]
        int e = rankOfSuffix[index];
        // Prm[d] <- e + Count[e]
        rankOfSuffix[index] = e + nextFreeIndexInBucket[e];
        // Count[e] <- Count[e] + 1
        nextFreeIndexInBucket[e]++;
        // B2H[Prm[d]] <- true
        smallestSuffixIn2hBucket[rankOfSuffix[index]] = true;
    }

    /**
     * min(j: j > Prm[ d ] and (BH[ j ] or !B2H[ j ])
     *
     * @param leftBucketBoundary Left boundary of bucket
     * @return Right boundary of the bucket
     */
    private int rightBucketBoundary(int leftBucketBoundary) {
        for (int j = leftBucketBoundary; j < textLength; j++) {
            if (smallestSuffixInBucket[j] || !smallestSuffixIn2hBucket[j]) {
                return j;
            }
        }

        return textLength;
    }

    /**
     * Computes bucket intervals
     * Intervals are in the form of Bucket start => next bucket start
     * First Bucket (index 0) holds the value for the next bucket start
     */
    private void computeBucketIntervals() {
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
        }
    }

    /**
     * Reset rankOfSuffix (PRM) array to point to the leftmost cell of the H-bucket containing the ith suffix
     * Assigns the left boundary of the interval to the corresponding bucket entries
     */
    private void resetRankOfSuffixArray() {
        // For each interval
        // l = start of bucket (left interval boundary)
        for (int l = 0; l < textLength; l = intervals[l]) {
            nextFreeIndexInBucket[l] = 0;

            // For each suffix in bucket
            // assign left boundary (l) to the suffix in bucket
            // c \in [l, r]  => c will go from left to right bucket boundary
            for (int c = l; c < intervals[l]; c++) {
                rankOfSuffix[suffixAtRank[c]] = l;
            }
        }
    }

    /**
     * Write computed SA to out.txt
     *
     * @throws IOException if the named file exists but is a directory rather
     *                     than a regular file, does not exist but cannot be
     *                     created, or cannot be opened for any other reason
     */
    private void write() throws IOException {
        try (BufferedWriter out = new BufferedWriter(new FileWriter("out.txt"))) {
            out.write("Sorted suffixes:\n");
            out.write("Rank => Suffix\n");

            final List<String> suffixes = Arrays.stream(suffixAtRank)
                    .mapToObj(i -> i + "=" + text.substring(i) + "\n")
                    .collect(Collectors.toList());

            for (int i = 0; i < suffixes.size(); i++) {
                out.write(i + ": " + suffixes.get(i));
            }

            out.write("\n\n\nRank of Suffix (inverse SA)\n");
            out.write(Arrays.stream(rankOfSuffix).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
        }
    }
}
