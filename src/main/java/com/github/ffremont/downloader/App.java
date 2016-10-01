package com.github.ffremont.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        Path conf = System.getProperty("conf") == null ? Paths.get("./files.txt") : Paths.get(System.getProperty("conf"));
        Path films = System.getProperty("files") == null ? Paths.get("files") : Paths.get(System.getProperty("files"));
        int threads = System.getProperty("threads") == null ? 3 : Integer.valueOf(System.getProperty("threads"));

        if (!Files.exists(films)) {
            Files.createDirectory(films);
        }

        ExecutorService service = Executors.newFixedThreadPool(threads);
        try {
            List<String> lines = Files.readAllLines(conf);
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] split = line.split("::");
                if (split.length == 2) {
                    String title = split[0].trim();
                    String url = split[1].trim();
                    service.submit(new Downloader(title, url, films));
                } else {
                    LOGGER.info("La ligne {} n'est pas lisible", line);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Impossible de récupérer la liste des fichiers", ex);
        } finally{
            service.shutdown();
        }
    }
}
