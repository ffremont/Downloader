package com.github.ffremont.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import static spark.Spark.before;
import static spark.Spark.delete;
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

    private static final List<String> resourcesWhiteList = Arrays.asList(".js", ".css", ".png", ".json", ".ico");

    public static ConcurrentHashMap<String, Metadata> launch = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Future<?>> workers = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<String> mustStop = new ConcurrentLinkedQueue<>();

    private static Path conf;
    private static Path files;
    public static int retry;

    public static boolean isBlocked(String title) {
        return launch.containsKey(title) ? launch.get(title).getTentative() >= retry : false;
    }

    public static void main(String[] args) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("header.txt");
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            System.out.print(scanner.useDelimiter("\\A").next());
            System.out.println("\n\n");
        }

        App.conf = System.getProperty("conf") == null ? Paths.get("files.txt") : Paths.get(System.getProperty("conf"));
        files = System.getProperty("files") == null ? Paths.get("files") : Paths.get(System.getProperty("files"));
        final int threads = System.getProperty("threads") == null ? 2 : Integer.valueOf(System.getProperty("threads"));
        final int delay = System.getProperty("delay") == null ? 30 : Integer.valueOf(System.getProperty("delay"));
        final int port = System.getProperty("port") == null ? 4567 : Integer.valueOf(System.getProperty("port"));
        retry = System.getProperty("retry") == null ? 3 : Integer.valueOf(System.getProperty("retry"));

        if (!Files.exists(files)) {
            Files.createDirectory(files);
        }
        if (!Files.exists(App.conf)) {
            Files.createFile(App.conf);
        }
        ExecutorService service = Executors.newFixedThreadPool(threads);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                LOGGER.debug("relecture du fichier de configuration");
                List<String> lines = Files.readAllLines(conf);
                for (String line : lines) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] split = line.split("::");
                    if (split.length == 2) {
                        String title = split[0].trim();
                        if(title.chars().filter(num -> num == '.').count() >= 2){
                            String titleOnly = title.substring(0, title.lastIndexOf("."));
                            title = titleOnly.replaceAll("\\.", " ")+title.substring(title.lastIndexOf("."));
                        }
                        
                        String url = split[1].trim();
                        final String tTitle = new String(title);
                        if (!workers.containsKey(title) && !isBlocked(title) && !Files.list(files).anyMatch(file -> file.toFile().getName().contains(tTitle+"."))) {
                            launch.putIfAbsent(title, new Metadata());
                            workers.put(title, service.submit(new Downloader(title, url, files)));
                        }
                    } else {
                        LOGGER.warn("La ligne {} n'est pas lisible", line);
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Impossible de récupérer la liste des fichiers", ex);
            }
        }, 0, delay, TimeUnit.SECONDS);

        port(port);
        LOGGER.info("Serveur accessible sur le port {}", port);

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
        get("/space", (request, response) -> {
            long freeSpace = files.toFile().getFreeSpace();
            long totalSpace = files.toFile().getTotalSpace();
            float percent = (float) (100 - (totalSpace > 0 ? Math.round(((float)freeSpace/totalSpace)*10000) / 100.0 : 0.0));
            
            return new SpaceFile(percent);
        }, new JsonTransformer());
        delete("/data/files/:title", (request, response) -> {
            LOGGER.debug(request.params("title"));

            String title = request.params("title");
            if (!mustStop.contains(title) && workers.containsKey(title)) {
                mustStop.add(title);
            }

            return "";
        });
        get("/resources/*", (request, response) -> {
            if (!resourcesWhiteList.stream().anyMatch(prefixe -> request.uri().endsWith(prefixe))) {
                halt(403);
            }

            if (request.uri().contains(".")) {
                String mime = Files.probeContentType(Paths.get(request.uri()));

                response.header("Content-Type", mime);
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(request.uri().replace("/resources/", ""));
            } else {
                halt(421);
            }

            return "";
        });
        get("/data/files", (request, response) -> {
            Map<String, Item> items = new HashMap<>();
            
            for(Map.Entry<String, Metadata> entry : launch.entrySet()){
                float advance = workers.containsKey(entry.getKey()) ? 0 : -1;
                
                Metadata metaData = entry.getValue();
                String[] tags = {metaData.getExtension()};
                
                if (metaData.getSize() != -1) {
                    try {
                        if(metaData.getDestination() != null){
                            advance = (float) Files.size(metaData.getDestination()) / (float) metaData.getSize();
                        }
                    } catch (IOException ex) {
                        LOGGER.error("oups", ex);
                    }
                }
                
                items.putIfAbsent(entry.getKey(), new Item(
                    entry.getKey(),
                    Item.getCompleteTitle(entry.getKey(), metaData.getSize()),
                    null,
                    advance,
                    tags)
                );
            }

            Files.walk(files).
                    sorted((a, b) -> b.compareTo(a)). // reverse; files before dirs
                    forEach((Path p) -> {
                        if (p.toFile().isFile()) {
                            String filename = p.getFileName().toString();
                            String title =  filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename;

                            List<String> tags = Arrays.asList();
                            if (filename.contains(".") && (filename.lastIndexOf(".")+1 < filename.length())) {
                                String extension = filename.substring(filename.lastIndexOf(".")+1);
                                tags = Arrays.asList(extension);
                            }
                            String[] rTags = (String[]) tags.toArray();
                            Item item = new Item(title, title, null, 1, rTags);

                            try {
                                item.setLabel(Item.getCompleteTitle(title, Files.size(p)));
                            } catch (IOException ex) {
                                LOGGER.error("impossible de récupérer la taille de {}", p.toAbsolutePath().toString(), ex);
                            }

                            if(!workers.containsKey(filename) && !workers.containsKey(title)){
                                if(launch.containsKey(title)){
                                    items.remove(title);
                                }
                                
                                items.putIfAbsent(filename, item);
                                items.put(filename, item);
                            }
                        }
                    });

            return items.values();
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
