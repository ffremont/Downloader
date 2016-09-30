/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.downloader;

import java.io.IOException;

/**
 *
 * @author florent
 */
public class FailedToDownloadException extends Exception{
    
    public FailedToDownloadException(String msg){
        super(msg);
    }

    FailedToDownloadException(String msg, IOException ex) {
        super(msg,ex);
    }
    
}
