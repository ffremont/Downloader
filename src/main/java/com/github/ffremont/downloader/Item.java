/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.downloader;

import java.text.DecimalFormat;

/**
 *
 * @author florent
 */
public class Item {

    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;

    private String title;
    private String[] tags;
    private String body;
    /**
     * -1 : error 0 : pas téléchargé 1 : téléchargé
     */
    private float download;

    public Item(String title, String body, float download, String[] tags) {
        this.title = title;
        this.body = body;
        this.download = download;
        this.tags = tags;
    }

    private static String convertToStringRepresentation(final long value) {
        final long[] dividers = new long[]{T, G, M, K, 1};
        final String[] units = new String[]{"TB", "GB", "MB", "KB", "B"};
        if (value < 1) {
            throw new IllegalArgumentException("Invalid file size: " + value);
        }
        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value,
            final long divider,
            final String unit) {
        final double result
                = divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }

    public static String getCompleteTitle(final String title, final long size) {
        if(size > -1){
        return title + "<span class=\"size\"> ("+convertToStringRepresentation(size)+")</span>";
        }else{
            return title;
        }
    }
    
    public static String getCompleteTitle(String title) {
        return getCompleteTitle(title, -1);
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
