package com.ido.idoprojectapp;

public class Chat {
    private String name;
    private int id;
    private Model model;

    //for later this is the way chats are gonna be saved and opened via drawer
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
