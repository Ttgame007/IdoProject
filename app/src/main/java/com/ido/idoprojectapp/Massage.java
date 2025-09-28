package com.ido.idoprojectapp;

public class Massage {
    private String content;
    private int sender; //1 for ai, 0 for user.
    public Massage(String content, int sender) {
        this.content = content;
        this.sender = sender;
    }
    public String getContent() {
        return content;
    }
    public int getSender() {
        return sender;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public void setSender(int sender) {
        this.sender = sender;
    }
}
