package com.github.ffremont.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Hello world!
 *
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static List<String> downloading = new ArrayList<>();
    private static Path conf;

    public static void main(String[] args) throws IOException {
        App.conf = System.getProperty("conf") == null ? Paths.get("files.txt") : Paths.get(System.getProperty("conf"));
        final Path films = System.getProperty("files") == null ? Paths.get("files") : Paths.get(System.getProperty("files"));
        final int threads = System.getProperty("threads") == null ? 3 : Integer.valueOf(System.getProperty("threads"));
        final int delay = System.getProperty("delay") == null ? 15 : Integer.valueOf(System.getProperty("delay"));
        final int port = System.getProperty("port") == null ? 4567 : Integer.valueOf(System.getProperty("port"));

        if (!Files.exists(films)) {
            Files.createDirectory(films);
        }
        ExecutorService service = Executors.newFixedThreadPool(threads);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Relecture du fichier de configuration");
                List<String> lines = Files.readAllLines(conf);
                for (String line : lines) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] split = line.split("::");
                    if (split.length == 2) {
                        String title = split[0].trim();
                        String url = split[1].trim();
                        if(!downloading.contains(title)){
                            changeStateOfDownload(title, true);
                            service.submit(new Downloader(title, url, films));
                        }
                    } else {
                        LOGGER.info("La ligne {} n'est pas lisible", line);
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Impossible de récupérer la liste des fichiers", ex);
            }
        }, 0, delay, TimeUnit.SECONDS);
        
        port(port);
        get("/", (request, response)-> {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.html");
        });
        get("/conf", (request, response)-> {
            return Files.newInputStream(App.conf.toAbsolutePath());
        });
        post("/conf", (request, response)-> {
            synchronized(App.class){
                Files.write(App.conf.toAbsolutePath(), request.bodyAsBytes());
            }
            
            return "";
        });

    }

    /**
     * Change l'état du téléchargement
     * 
     * @param download
     * @param isDownloading 
     */
    synchronized public static void changeStateOfDownload(String download, boolean isDownloading) {
        if(isDownloading){
            downloading.add(download);
        }else{
            downloading.remove(download);
        }
    }
}
