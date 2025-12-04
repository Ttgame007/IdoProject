package com.ido.idoprojectapp.deta.model;

public class Chat {
    private final String name;
    private final int id;
    private final Model model;

    public Chat(String name, int id, Model model) {
        this.name = name;
        this.id = id;
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }
    public String loadModel() {
        return model.getPath();

    }
}