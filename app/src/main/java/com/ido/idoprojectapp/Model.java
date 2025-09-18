package com.ido.idoprojectapp;

public class Model {

    private final String path;
    private final String name;

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
}
