package com.example.myapplication;

public class ImageInfo {
    private String fileName;
    private String url;

    public ImageInfo(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUrl() {
        return url;
    }
}
