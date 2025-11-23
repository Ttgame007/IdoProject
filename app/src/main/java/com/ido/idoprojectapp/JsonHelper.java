package com.ido.idoprojectapp;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonHelper {

    private static final Gson gson = new Gson();

    // ====== File I/O ======

    private static String readFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private static void writeFile(File file, String data) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(data);
        fw.close();
    }

    // ====== Directory Management ======

    public static boolean deleteUserDirectory(Context ctx, String username) {
        File dir = new File(ctx.getFilesDir(), "user_" + username);
        return deleteRecursive(dir);
    }

    private static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        return fileOrDirectory.delete();
    }

    public static boolean renameUserDirectory(Context ctx, String oldUsername, String newUsername) {
        File oldDir = new File(ctx.getFilesDir(), "user_" + oldUsername);
        File newDir = new File(ctx.getFilesDir(), "user_" + newUsername);

        if (oldDir.exists()) {
            return oldDir.renameTo(newDir);
        }
        return false;
    }

    // ====== Chat Operations ======

    public static List<Chat> loadChats(Context ctx, String username) {
        try {
            File file = new File(ctx.getFilesDir(), "user_" + username + "/chats.json");
            if (!file.exists()) return new ArrayList<>();
            Type type = new TypeToken<List<Chat>>(){}.getType();
            return gson.fromJson(readFile(file), type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveChats(Context ctx, String username, List<Chat> chats) {
        try {
            File dir = new File(ctx.getFilesDir(), "user_" + username);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "chats.json");
            writeFile(file, gson.toJson(chats));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeChat(Context ctx, String username, int chatId) {
        try {
            File dir = new File(ctx.getFilesDir(), "user_" + username);
            if (!dir.exists()) return;

            List<Chat> chats = loadChats(ctx, username);

            Chat chatToRemove = null;
            for (Chat chat : chats) {
                if (chat.getId() == chatId) {
                    chatToRemove = chat;
                    break;
                }
            }

            if (chatToRemove != null) {
                chats.remove(chatToRemove);
                saveChats(ctx, username, chats);
                new File(dir, "chat_" + chatId + ".json").delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====== Message Operations ======

    public static List<Message> loadMessages(Context ctx, String username, int chatId) {
        try {
            File file = new File(ctx.getFilesDir(), "user_" + username + "/chat_" + chatId + ".json");
            if (!file.exists()) return new ArrayList<>();
            Type type = new TypeToken<List<Message>>(){}.getType();
            return gson.fromJson(readFile(file), type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveMessages(Context ctx, String username, int chatId, List<Message> messages) {
        try {
            File dir = new File(ctx.getFilesDir(), "user_" + username);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "chat_" + chatId + ".json");
            writeFile(file, gson.toJson(messages));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}