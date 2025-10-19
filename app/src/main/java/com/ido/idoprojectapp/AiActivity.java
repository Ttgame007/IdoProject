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
    private ImageButton logoutIcon, menuIcon;
    private TextView WelcomeText, profileName, TVguest;
    private EditText ETinput;
    private DrawerLayout drawerLayout;
    private ConstraintLayout layout;
    private RecyclerView chatList;
    private RecyclerView messageList;
    private MessageAdapter messageAdapter;
    private List<Chat> chats = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private boolean isInChat;
    private ChatAdapter chatAdapter;
    private PrefsHelper prefs;
    private String aiOutput;
    private int currentChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai);

        prefs = new PrefsHelper(this);

        // --- UI References ---
        WelcomeText = findViewById(R.id.welcomeText);
        TVguest = findViewById(R.id.TVguest);
        ETinput = findViewById(R.id.ETinput);
        BTNsend = findViewById(R.id.BTNsend);
        drawerLayout = findViewById(R.id.main);
        layout = findViewById(R.id.content);
        chatList = findViewById(R.id.chatList);
        messageList = findViewById(R.id.messageList);
        newChatBtn = findViewById(R.id.newChatBtn);
        menuIcon = findViewById(R.id.menuIcon);
        logoutIcon = findViewById(R.id.logoutIcon);
        profileName = findViewById(R.id.profileName);
        TVguest.setText(prefs.getUsername());
        profileName.setText(prefs.getUsername());
        // Setup RecyclerViews
        messageAdapter = new MessageAdapter(messages);
        messageList.setAdapter(messageAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(AiActivity.this));

        chats = JsonHelper.loadChats(this, prefs.getUsername());
        chatAdapter = new ChatAdapter(chats, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(Chat chat) {
                currentChat = chat.getId();
                WelcomeText.setVisibility(View.GONE);
                TVguest.setVisibility(View.GONE);
                isInChat = true;
                messages.clear();
                messages.addAll(JsonHelper.loadMessages(AiActivity.this, prefs.getUsername(), chat.getId()));
                messageAdapter.notifyDataSetChanged();
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onChatLongClick(Chat chat) {
                showDeleteChatDialog(chat);
            }
        });

        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
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
            if (!isInChat) {
                WelcomeText.setVisibility(View.GONE);
                TVguest.setVisibility(View.GONE);
                isInChat = true;
                //creates new chat loads it and sends the message there
                Chat newChat = new Chat("Chat " + (chats.size() + 1), chats.size() + 1, null);
                chats.add(newChat);
                JsonHelper.saveChats(this, prefs.getUsername(), chats);
                chatAdapter.notifyItemInserted(chats.size() - 1);
                currentChat = newChat.getId();
                messages.clear();
                messages.add(new Message("", 1));
                messageAdapter.notifyItemInserted(messages.size() - 1);
            }
            String userInput = ETinput.getText().toString();
            if (userInput.isEmpty()) return;
            String input = "user: " + userInput + "\n Assistant: ";
            messages.add(new Message(userInput, 0));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messageList.scrollToPosition(messages.size() - 1);

            ETinput.setText("");
            ETinput.clearFocus();

            int aiMessagePosition = messages.size() - 1;
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messageList.scrollToPosition(messages.size() - 1);

            // Send message to AI
            llmw.send(input, msg -> runOnUiThread(() -> {
                Message aiMessage = messages.get(aiMessagePosition);
                String currentContent = aiMessage.getContent();
                aiMessage.setContent(currentContent + msg);
                messageAdapter.notifyItemChanged(aiMessagePosition);
                aiOutput = msg;
                Log.d("input", input);
                Log.d("output", msg);
                JsonHelper.saveMessages(this, prefs.getUsername(), currentChat, messages);
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
                    int chatIdToDelete = chat.getId();
                    int index = chats.indexOf(chat);
                    if (index != -1) {
                        JsonHelper.removeChat(this, prefs.getUsername(), chatIdToDelete);
                        chats.remove(index);
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