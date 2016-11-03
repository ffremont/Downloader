/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.downloader;

import java.nio.file.Path;

/**
 *
 * @author florent
 */
public class Metadata {
    private long size;
    private Path temp;
    private String filename;
    private String extension;
    private int tentative;
    
    private boolean rangesRequest;
    private int downloaded;

    public Metadata() {
        this.tentative = 0;
        this.size = -1;
        this.downloaded = 0;
        this.rangesRequest = true;
    }

    public boolean isRangesRequest() {
        return rangesRequest;
    }

    public void setRangesRequest(boolean rangesRequest) {
        this.rangesRequest = rangesRequest;
    }
    
    public int getTentative() {
        return tentative;
    }

    public void setTentative(int tentative) {
        this.tentative = tentative;
    }
    
    public int getDownloaded() {
        return downloaded;
    }
    
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Path getTemp() {
        return temp;
    }

    public void setTemp(Path temp) {
        this.temp = temp;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    void download(int nRead) {
        this.downloaded += nRead;
    }
    
}
