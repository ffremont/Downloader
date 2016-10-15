package com.github.ffremont.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * Hello world!
 *
 */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static ConcurrentHashMap<String, Metadata> downloading = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<String> blacklist = new ConcurrentLinkedQueue<>();
    private static Path conf;
    private static Path films;

    public static void main(String[] args) throws IOException {
        App.conf = System.getProperty("conf") == null ? Paths.get("files.txt") : Paths.get(System.getProperty("conf"));
        films = System.getProperty("files") == null ? Paths.get("files") : Paths.get(System.getProperty("files"));
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
                        if (!downloading.containsKey(title) && !blacklist.contains(title)) {
                            downloading.put(title, new Metadata());
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
        before((request, response) -> {
            if ("/".equals(request.uri())) {
                Spark.redirect.get("/", "/files");
            }
        });
        get("/conf", (request, response) -> {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.html");
        });

        get("/files", (request, response) -> {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("download.html");
        });
        get("/resources/*", (request, response) -> {

            boolean isJs = request.uri().endsWith(".js"), isCss = request.uri().endsWith(".css");
            if (isJs || isCss) {
                if (isJs) {
                    response.header("Content-Type", "text/javascript");
                } else if (isCss) {
                    response.header("Content-Type", "text/css");
                }

                return Thread.currentThread().getContextClassLoader().getResourceAsStream(request.uri().replace("/resources/", ""));
            }

            halt(404);
            return "";
        });
        get("/data/files", (request, response) -> {
            List<Item> items = new ArrayList<>();

            for (Entry<String, Metadata> entry : downloading.entrySet()) {
                Metadata metaData = entry.getValue();
                int advance = 0;
                if ((metaData != null) && (metaData.getTemp() != null)) {
                    if (metaData.getSize() == -1) {
                        advance = -1;
                    } else {
                        try {
                            advance = Math.round(Files.size(metaData.getTemp()) / metaData.getSize());
                        } catch (IOException ex) {
                            LOGGER.error("oups", ex);
                        }
                    }
                }

                String[] tags = {metaData.getExtension()};
                if(blacklist.contains(entry.getKey())){
                    advance = -1;
                }
                items.add(new Item(entry.getKey(), null, advance, tags));
            }
            
            for(String inFaildTitle : blacklist){
                if(!downloading.containsKey(inFaildTitle)){
                    items.add(new Item(inFaildTitle, null, -1, null));
                }
            }

            Files.walk(films).
                    sorted((a, b) -> b.compareTo(a)). // reverse; files before dirs
                    forEach((Path p) -> {
                        if (p.toFile().isFile()) {
                            String filename = p.getFileName().toString();
                            String[] tab = p.getFileName().toString().split("\\.");
                            String title = tab[0];
                            
                            String[] tags = {tab[1]};
                            items.add(new Item(title, null, 1, tags));
                        }
                    });

            return items;
        }, new JsonTransformer());

        get("/data/conf", (request, response) -> {
            return Files.newInputStream(App.conf.toAbsolutePath());
        });
        post("/data/conf", (request, response) -> {
            synchronized (App.class) {
                Files.write(App.conf.toAbsolutePath(), request.bodyAsBytes());
            }

            return "";
        });

    }
}
