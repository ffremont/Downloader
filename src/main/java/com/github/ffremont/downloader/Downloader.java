/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author florent
 */
public class Downloader implements Runnable {

    private final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.63 Safari/537.36";

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    private String title;
    private String url;
    private Path dest;

    public Downloader(String title, String url, Path dest) {
        this.title = title;
        this.url = url;
        this.dest = dest;
    }

    private HttpURLConnection navigateTo(String url) throws FailedToDownloadException {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            LOGGER.debug("fichier à l'adresse '{}', statut HTTP  {}", url, responseCode);
            if (responseCode == 302 || responseCode == 301) {
                String location = con.getHeaderField("Location") == null ? con.getHeaderField("location") : con.getHeaderField("Location");
                LOGGER.debug("location => {}", location);
                return navigateTo(location);
            } else if (responseCode == 200) {
                LOGGER.info("téléchargement avec succès du fichier '{}'", title);
                return con;
            } else {
                throw new FailedToDownloadException("Status de la réponse invalide "+responseCode);
            }
        } catch (IOException ex) {
            throw new FailedToDownloadException("Téléchargement du fichier " + title + " impossible", ex);
        }
    }

    public void run() {
        try {
            LOGGER.debug("tentative de téléchargement du fichier '{}'", title);
            HttpURLConnection con = navigateTo(url);

            TikaConfig config = TikaConfig.getDefaultConfig();
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType videoMime = allTypes.forName(con.getHeaderField("Content-Type"));
            String extension = videoMime.getExtension();

            if (title.contains(".")) {
                String[] splitTitle = title.split("\\.");
                if (splitTitle.length > 1) {
                    extension = "."+splitTitle[splitTitle.length - 1];
                }
            }

            LOGGER.debug(con.getHeaderFields().entrySet().toString());
            App.downloading.get(title).setSize(con.getHeaderFieldLong("Content-Length", -1));

            String finalFilename = title + extension;
            App.downloading.get(title).setFilename(finalFilename);
            App.downloading.get(title).setExtension(extension);
            if (Files.exists(Paths.get(dest.toAbsolutePath().toString(), finalFilename))) {
                LOGGER.debug("le fichier '{}' existe déjà", finalFilename);
            } else {
                Path tmpFilm = Files.createTempFile("file_", "_downloader");
                App.downloading.get(title).setTemp(tmpFilm);
                FileOutputStream out = new FileOutputStream(tmpFilm.toFile());

                try (InputStream is = con.getInputStream()) {
                    int nRead;
                    byte[] data = new byte[10240];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        out.write(data, 0, nRead);
                    }
                    out.flush();
                    is.close();
                }
                Files.copy(
                        tmpFilm,
                        Paths.get(dest.toAbsolutePath().toString(), finalFilename)
                );
                Files.delete(tmpFilm);
                LOGGER.info("copie avec succès du fichier '{}' dans le répertoire cible", title);
            }

        } catch (FailedToDownloadException | IOException | MimeTypeException ex) {
            App.blacklistRetry.putIfAbsent(title, 0);
            
            App.blacklistRetry.replace(title, App.blacklistRetry.get(title) + 1);
            LOGGER.error("Téléchargement du fichier '" + title + "' impossible", ex);
        } finally {
            App.downloading.remove(title);
            App.workers.remove(title);
        }
    }

}
