package de.octofox.suffixarray;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please add the filepath to load as the first argument.");
            return;
        }

        final String filename = args[0];
        final Path filepath = Paths.get(filename);
        byte[] data;

        try {
            data = Files.readAllBytes(filepath);
        } catch (IOException e) {
            System.err.println("Could not read file " + filepath.toString());
            return;
        }

        final String text = new String(data, StandardCharsets.UTF_8);

        SuffixArray suffixArray = new SuffixArray(text);

    }
}
