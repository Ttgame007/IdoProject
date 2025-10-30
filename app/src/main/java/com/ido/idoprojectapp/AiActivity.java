package com.ido.idoprojectapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.Nullable;
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

    private static final int MODEL_SETTINGS_REQUEST_CODE = 1;
    private LLMW llmw;
    private Button BTNsend, newChatBtn, profileBtn, modelSettingsBtn;
    private ImageButton logoutIcon, menuIcon, settingIcon;
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
        profileBtn = findViewById(R.id.profileNameBtn);
        modelSettingsBtn = findViewById(R.id.modelSettingsBtn);
        drawerLayout = findViewById(R.id.main);
        layout = findViewById(R.id.content);
        chatList = findViewById(R.id.chatList);
        messageList = findViewById(R.id.messageList);
        newChatBtn = findViewById(R.id.newChatBtn);
        menuIcon = findViewById(R.id.menuIcon);
        logoutIcon = findViewById(R.id.logoutIcon);
        settingIcon = findViewById(R.id.settingIcon);
        profileName = findViewById(R.id.profileNameBtn);
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
        modelSettingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(AiActivity.this, ModelSettingsActivity.class);
            startActivityForResult(intent, MODEL_SETTINGS_REQUEST_CODE);
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
        settingIcon.setOnClickListener(v -> {
            Intent intent = new Intent(AiActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        // AI model setup
        // THIS SECTION IS INTENTIONALLY LEFT BLANK.
        // The model is now loaded only in ModelSettingsActivity to prevent delays here.
        // onResume() will handle checking if the model is ready.

        // Send button
        BTNsend.setOnClickListener(v -> {
            // Safety check in case the UI state is inconsistent
            if (llmw == null || !LLMW.Companion.isLoaded()) {
                Toast.makeText(this, "Cannot send: AI model is not active.", Toast.LENGTH_SHORT).show();
                return;
            }

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
                messageAdapter.notifyDataSetChanged();
            }

            String userInput = ETinput.getText().toString().trim();
            if (userInput.isEmpty()) return;

            String input = "user: " + userInput + "\n Assistant: ";
            // Add the user's message
            messages.add(new Message(userInput, 0));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messageList.scrollToPosition(messages.size() - 1);

            ETinput.setText("");
            ETinput.clearFocus();

            // Add a blank placeholder for the AI's response
            messages.add(new Message("", 1));
            int aiMessagePosition = messages.size() - 1; // This is now the correct position
            messageAdapter.notifyItemInserted(aiMessagePosition);
            messageList.scrollToPosition(aiMessagePosition);

            // Send message to AI
            llmw.send(input, msg -> runOnUiThread(() -> {
                // Get the message from the correct position and append new text
                Message aiMessage = messages.get(aiMessagePosition);
                String currentContent = aiMessage.getContent();
                aiMessage.setContent(currentContent + msg);
                messageAdapter.notifyItemChanged(aiMessagePosition);
                aiOutput = msg;
                Log.d("input", input);
                Log.d("output", msg);
                // Save messages on each update to ensure no data loss
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MODEL_SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            refreshModelState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModelState();
    }

    private void setUiEnabled(boolean isEnabled) {
        BTNsend.setEnabled(isEnabled);
        ETinput.setEnabled(isEnabled);
    }

    private void refreshModelState() {
        if (LLMW.Companion.isLoaded()) {
            Log.d("AiActivity", "Model is loaded. Enabling UI.");
            setUiEnabled(true);
            ETinput.setHint("Type your message...");


            if (llmw == null) {
                llmw = LLMW.Companion.getInstance(null);
            }
        } else {
            Log.d("AiActivity", "No model loaded. Disabling UI.");
            setUiEnabled(false);
            ETinput.setHint("Download a model in settings");
            llmw = null;
        }
    }
}