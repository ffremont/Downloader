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

    private static final String RANGE_REQUEST_HEADER = "Accept-Ranges";
    private static final int BUFFER_SIZE = 10240;

    private String title;
    private String url;
    private Path dest;

    public Downloader(String title, String url, Path dest) {
        this.title = title;
        this.url = url;
        this.dest = dest;
    }

    private HttpURLConnection navigateTo(String url, Metadata meta) throws FailedToDownloadException {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);
            if (meta.isRangesRequest() && (meta.getSize() > -1)) {
                String byteRange = meta.getDownloaded() + "-" + meta.getSize();
                con.setRequestProperty("Range", "bytes=" + byteRange);
            }

            int responseCode = con.getResponseCode();
            LOGGER.debug("fichier à l'adresse '{}', statut HTTP  {}", url, responseCode);
            if (responseCode == 302 || responseCode == 301) {
                String location = con.getHeaderField("Location") == null ? con.getHeaderField("location") : con.getHeaderField("Location");
                LOGGER.debug("location => {}", location);
                return navigateTo(location, meta);
            } else if (((responseCode % 200) < 200) && ((responseCode / 200) == 1)) {
                LOGGER.info("téléchargement avec succès du fichier '{}'", title);
                if (con.getHeaderField(RANGE_REQUEST_HEADER) != null) {
                    meta.setRangesRequest(con.getHeaderField(RANGE_REQUEST_HEADER).contains("bytes"));
                }

                return con;
            } else {
                throw new FailedToDownloadException("Status de la réponse invalide " + responseCode);
            }
        } catch (IOException ex) {
            throw new FailedToDownloadException("Téléchargement du fichier " + title + " impossible", ex);
        }
    }

    public void run() {
        Metadata meta = App.launch.get(title);
        try {
            LOGGER.debug("tentative de téléchargement du fichier '{}'", title);

            HttpURLConnection con = navigateTo(url, meta);

            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType videoMime = allTypes.forName(con.getHeaderField("Content-Type"));
            String extension = videoMime.getExtension();

            String realTitle = new String(title);
            if (realTitle.contains(".")) {
                String[] splitTitle = realTitle.split("\\.");
                if (splitTitle.length > 1) {
                    extension = "." + splitTitle[splitTitle.length - 1];
                    realTitle = realTitle.substring(0, realTitle.lastIndexOf(".") - 1);
                }
            }

            LOGGER.debug(con.getHeaderFields().entrySet().toString());
            meta.setSize(con.getHeaderFieldLong("Content-Length", -1));

            String finalFilename = realTitle + extension;
            meta.setFilename(finalFilename);
            meta.setExtension(extension);
            if (Files.exists(Paths.get(dest.toAbsolutePath().toString(), finalFilename))) {
                LOGGER.debug("le fichier '{}' existe déjà", finalFilename);
            } else {
                Path tmpFilm = null;
                if (meta.getTemp() == null) {
                    tmpFilm = Files.createTempFile("file_", "_downloader");
                    meta.setTemp(tmpFilm);
                } else {
                    tmpFilm = meta.getTemp();
                }

                try {
                    FileOutputStream out = new FileOutputStream(tmpFilm.toFile(), true);

                    try (InputStream is = con.getInputStream()) {
                        int nRead;
                        int offset = meta.getDownloaded();
                        byte[] data = new byte[BUFFER_SIZE];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            if (App.mustStop.contains(title)) {
                                throw new StopDownload();
                            }
                            out.write(data, 0, nRead);
                            meta.download(nRead);
                            offset = 0;
                        }
                        out.flush();
                        is.close();
                    }
                    Files.copy(
                            tmpFilm,
                            Paths.get(dest.toAbsolutePath().toString(), finalFilename)
                    );

                } finally {
                    if (meta.getDownloaded() >= meta.getSize()) {
                        LOGGER.debug("Fichier complet et copié dans le répertoire cible");
                        LOGGER.debug("suppression du fichier temporaire {}", meta.getTemp().toAbsolutePath());
                        Files.delete(tmpFilm);
                    } else {
                        LOGGER.info("Téléchargement partiel de {}", title);
                    }
                }

            }
        } catch (FailedToDownloadException | IOException | MimeTypeException ex) {
            meta.setTentative(meta.getTentative() + 1);
            LOGGER.error("Téléchargement du fichier '" + title + "' impossible", ex);
        } finally {
            App.workers.remove(title);
            App.mustStop.remove(title);
            LOGGER.debug("Arrêt du téléchargement de {}", title);
        }
    }

}
