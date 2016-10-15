/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.downloader;

/**
 *
 * @author florent
 */
public class Item {
    private String title;
    private String[] tags;
    private String body;
    /**
     * -1 : error
     * 0 : pas téléchargé
     * 1 : téléchargé
     */
    private float download;

    public Item(String title, String body, float download, String[] tags) {
        this.title = title;
        this.body = body;
        this.download = download;
        this.tags = tags;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public float getDownload() {
        return download;
    }

    public void setDownload(float download) {
        this.download = download;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
