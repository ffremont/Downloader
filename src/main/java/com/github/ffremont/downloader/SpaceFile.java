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
public class SpaceFile {
    private float percentUsed;

    public SpaceFile(float percentUsed) {
        this.percentUsed = percentUsed;
    }

    public float getPercentUsed() {
        return percentUsed;
    }

    public void setPercentUsed(float percentUsed) {
        this.percentUsed = percentUsed;
    }

    
}
