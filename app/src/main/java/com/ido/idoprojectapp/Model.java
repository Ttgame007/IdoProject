package com.ido.idoprojectapp;

import java.io.Serializable;

public class Model implements  Serializable {

    private final String path;
    private final String name;
    private String size;
    private String description;
    private String filename;


    public Model(String path, String name, String size, String description, String filename) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.description = description;
        this.filename = filename;
    }
    public Model(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }
    public String getSize() {
        return size;
    }
    public String getDescription() {
        return description;
    }
    public String getFilename() {
        return filename;
    }
}
