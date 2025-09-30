package com.ido.idoprojectapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AiActivity extends AppCompatActivity {

    private LLMW llmw;
    private Button BTNsend, newChatBtn;
    private ImageButton logoutIcon;
    private TextView TVoutput, profileName, TVguest;
    private EditText ETinput;
    private DrawerLayout drawerLayout;
    private ConstraintLayout layout;
    private RecyclerView chatList;

    private List<Chat> chats = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai);

        prefs = new PrefsHelper(this);

        // --- UI References ---
        TVguest = findViewById(R.id.TVguest);
        TVoutput = findViewById(R.id.textView);
        ETinput = findViewById(R.id.ETinput);
        BTNsend = findViewById(R.id.BTNsend);
        drawerLayout = findViewById(R.id.main);
        layout = findViewById(R.id.content);
        chatList = findViewById(R.id.chatList);
        newChatBtn = findViewById(R.id.newChatBtn);
        logoutIcon = findViewById(R.id.logoutIcon);
        profileName = findViewById(R.id.profileName);

        TVguest.setText(prefs.getUsername());
        profileName.setText(prefs.getUsername());
        // Setup RecyclerView drawer
        chats = JsonHelper.loadChats(this, prefs.getUsername());
        chatAdapter = new ChatAdapter(chats, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(Chat chat) {
                // TODO: load chat messages into RecyclerView in main content
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onChatLongClick(Chat chat) {
                showDeleteChatDialog(chat);
            }
        });
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(chatAdapter);

        // New chat button
        newChatBtn.setOnClickListener(v -> {
            Chat newChat = new Chat("Chat " + (chats.size() + 1), chats.size() + 1, null);
            chats.add(newChat);
            JsonHelper.saveChats(this, prefs.getUsername(), chats);
            chatAdapter.notifyItemInserted(chats.size() - 1);
        });

        // Logout listener attached to the icon
        logoutIcon.setOnClickListener(v -> logOut());

        // AI model setup
        try {
            llmw = LLMW.Companion.getInstance(preparePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Send button
        BTNsend.setOnClickListener(v -> {
            String input = "user: " + ETinput.getText().toString() + "\n Assistant: ";
            TVoutput.setText("");

            llmw.send(input, msg -> runOnUiThread(() -> {
                TVoutput.append(msg);
                Log.d("input", input);
                Log.d("output", msg);
                ETinput.setText("");
                ETinput.clearFocus();
            }));
        });
    }
    //i find this and below it really self explanatory

    private void logOut() {
        prefs.clearCardensials();
        finish();
    }

    private void showDeleteChatDialog(Chat chat) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Do you want to delete chat: " + chat.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int index = chats.indexOf(chat);
                    if (index != -1) {
                        chats.remove(index);
                        JsonHelper.saveChats(this, prefs.getUsername(), chats);
                        chatAdapter.notifyItemRemoved(index);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //prepere the path for the ai model in the chach
    private String preparePath() throws IOException {
        InputStream inputStream = getResources().openRawResource(R.raw.llama);
        File tempFile = File.createTempFile("myfile", ".gguf", getCacheDir());
        try (OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        inputStream.close();
        return tempFile.getAbsolutePath();
    }
}