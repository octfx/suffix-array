package de.octofox.suffixarray;

import de.octofox.suffixarray.search.KeywordInContext;
import de.octofox.suffixarray.search.NaiveSearch;
import de.octofox.suffixarray.search.Search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        final String input = parseArgs(args);

        if (input.length() > 0) {
            final SuffixArray sa = new SuffixArray(input);
            sa.compute();

            final KeywordInContext kwic = new KeywordInContext(sa, input);
            final NaiveSearch naive = new NaiveSearch(sa, input);
            Search s = kwic;

            while (true) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Type a substring to search for:");

                final String in = scanner.nextLine();
                if (in.equals("q")) {
                    break;
                } else if (in.equals("naive")) {
                    System.out.println("Using 'naive' search.");
                    s = naive;
                    continue;
                } else if (in.equals("kwic")) {
                    System.out.println("Using 'kwic' search.");
                    s = kwic;
                    continue;
                }

                s.search(in);
            }
        } else {
            System.out.println("Call jar with '-f filename' to use a local file to sort. Call with one arg: Argument is used as input string.");
        }
    }

    /**
     * Simple method to parse given jvm arguments
     *
     * @param args CLI args '-f filename' or 'StringToSort'
     * @return String to sort
     */
    private static String parseArgs(final String[] args) {
        if (args == null || args.length == 0 || args.length > 2) {
            return "";
        }

        if (args.length == 1) {
            return args[0];
        }

        return readFile(args[1]);
    }

    /**
     * Reads a file to string
     *
     * @param fileName The file to read
     * @return File as string
     */
    private static String readFile(final String fileName) {
        File file = new File(fileName);

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            return new String(fileBytes);
        } catch (IOException e) {
            System.out.println("Could not read file '" + fileName + "'." + e.getMessage());
        }

        return "";
    }
}
