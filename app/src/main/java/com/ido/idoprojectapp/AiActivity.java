package com.ido.idoprojectapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AiActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    private static final int REQUEST_VOICE_INPUT = 11;
    private static final int MODEL_SETTINGS_REQUEST_CODE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_CAPTURE_IMAGE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant. " +
                    "Provide direct, accurate, and concise answers. " +
                    "Do not continue the conversation yourself. " +
                    "Do not generate user questions or responses. " +
                    "Stop your response when you have fully answered the question.";

    private static final List<String> STOP_TOKENS = Arrays.asList(
            "\nuser:", "\nUser:", "\nUSER:",
            "\nhuman:", "\nHuman:", "\nHUMAN:",
            "\nquestion:", "\nQuestion:",
            "\n###", "### User", "### Human",
            "\n\n\n",
            "</s>", "<|im_end|>", "<|endoftext|>"
    );

    private LLMW llmw;
    private Button BTNsend, newChatBtn, profileBtn, modelSettingsBtn;
    private ImageButton logoutIcon, menuIcon, settingIcon, BTNmic;
    private TextView WelcomeText, TVguest;
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
    private int currentChat;
    private ImageView drawerProfileImage;

    private long lastMessageTime = 0;
    private int messageCount = 0;
    private HelperUserDB userDbHelper;

    // ====== Lifecycle ======

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsHelper(this);

        if (prefs.isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_ai);

        userDbHelper = new HelperUserDB(this);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                } else {
                    isTtsReady = true;
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });

        initializeViews();

        chats = JsonHelper.loadChats(this, prefs.getUsername());
        if (chats == null) chats = new ArrayList<>();

        setupRecyclerViews();
        setupClickListeners();
        loadProfileImageFromDb();
        autoLoadModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModelState();
        loadProfileImageFromDb();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MODEL_SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            refreshModelState();
            return;
        }

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQUEST_VOICE_INPUT) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    ETinput.setText(result.get(0));
                }
            }
            return;
        }

        Bitmap imageBitmap = null;

        if (requestCode == REQUEST_PICK_IMAGE) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                imageBitmap = (Bitmap) extras.get("data");
            }
        }

        if (imageBitmap != null) {
            byte[] profilePicBytes = convertBitmapToByteArray(imageBitmap);
            boolean isUpdated = userDbHelper.updateProfilePicture(prefs.getUsername(), profilePicBytes);

            if (isUpdated) {
                loadProfileImageFromDb();
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ====== UI Setup ======

    private void initializeViews() {
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
        drawerProfileImage = findViewById(R.id.drawerProfileImage);
        BTNmic = findViewById(R.id.BTNmic);
        TVguest.setText(prefs.getUsername());
        profileBtn.setText(prefs.getUsername());
    }

    private void setupRecyclerViews() {
        messageAdapter = new MessageAdapter(messages);
        messageList.setAdapter(messageAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(this));

        chatAdapter = new ChatAdapter(chats, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(Chat chat) {
                loadChat(chat);
            }

            @Override
            public void onChatLongClick(Chat chat) {
                showDeleteChatDialog(chat);
            }
        });
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(AiActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        if (drawerProfileImage != null) {
            drawerProfileImage.setOnClickListener(v -> showImageSourceDialog());
        }
        if (BTNmic != null) {
            BTNmic.setOnClickListener(v -> startVoiceInput());
        }

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

        newChatBtn.setOnClickListener(v -> createNewChat());
        logoutIcon.setOnClickListener(v -> logOut());

        settingIcon.setOnClickListener(v -> {
            Intent intent = new Intent(AiActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        settingIcon.setOnLongClickListener(v -> {
            showAIConfigDialog();
            return true;
        });

        BTNsend.setOnClickListener(v -> sendMessage());
        BTNsend.setOnLongClickListener(v -> {
            showDebugOptions();
            return true;
        });
    }

    private void setUiEnabled(boolean isEnabled) {
        BTNsend.setEnabled(isEnabled);
        ETinput.setEnabled(isEnabled);
    }

    private void refreshModelState() {
        if (LLMW.Companion.isLoaded()) {
            Log.d("AiActivity", "Model is loaded");
            setUiEnabled(true);
            ETinput.setHint("Type your message...");

            if (llmw == null) {
                llmw = LLMW.Companion.getInstance(null, prefs.getContextSize());
            }
        } else {
            Log.d("AiActivity", "No model loaded in RAM");
            String savedModel = prefs.getCurrentModelFilename();
            if (savedModel != null && !savedModel.isEmpty() && new File(getFilesDir(), savedModel).exists()) {
                autoLoadModel();
            } else {
                setUiEnabled(false);
                ETinput.setHint("Download a model in settings");
                llmw = null;
            }
        }
    }

    // ====== AI Handling ======

    private void sendMessage() {
        if (llmw == null || !LLMW.Companion.isLoaded()) {
            Toast.makeText(this, "AI model is not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String userInput = ETinput.getText().toString().trim();
        if (userInput.isEmpty()) return;

        if (!isInChat) {
            startNewChat();
        }

        long currentTime = System.currentTimeMillis();
        if (lastMessageTime > 0) {
            long timeSinceLastMessage = currentTime - lastMessageTime;
            Log.d("Performance", "Time since last message: " + timeSinceLastMessage + "ms");
        }
        lastMessageTime = currentTime;
        messageCount++;

        String prompt = buildPromptWithMemory(userInput);

        Log.d("Prompt", "Message #" + messageCount);
        Log.d("Prompt", "Length: " + prompt.length() + " chars (~" + (prompt.length() / 4) + " tokens)");

        messages.add(new Message(userInput, 0));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        messageList.scrollToPosition(messages.size() - 1);

        ETinput.setText("");
        ETinput.clearFocus();

        messages.add(new Message("", 1));
        int aiMessagePosition = messages.size() - 1;
        messageAdapter.notifyItemInserted(aiMessagePosition);
        messageList.scrollToPosition(aiMessagePosition);

        int maxTokens = prefs.getMaxResponseTokens();
        sendToModel(prompt, aiMessagePosition, maxTokens);
    }

    private String buildPromptWithMemory(String userInput) {
        StringBuilder prompt = new StringBuilder();
        int maxContextMessages = prefs.getMaxContextMessages();

        prompt.append("### System\n");
        prompt.append(SYSTEM_PROMPT).append("\n\n");

        if (!messages.isEmpty()) {
            prompt.append("### Previous Conversation\n");
            int messagesToInclude = Math.min(messages.size(), maxContextMessages);
            int startIdx = Math.max(0, messages.size() - messagesToInclude);

            for (int i = startIdx; i < messages.size(); i++) {
                Message m = messages.get(i);
                if (m.getSender() == 0) {
                    prompt.append("User: ").append(m.getContent()).append("\n");
                } else if (!m.getContent().isEmpty()) {
                    prompt.append("Assistant: ").append(m.getContent()).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("### Current Question\n");
        prompt.append("User: ").append(userInput).append("\n");
        prompt.append("Assistant:");

        return prompt.toString();
    }

    private void sendToModel(String prompt, int aiMessagePosition, int maxTokens) {
        long startTime = System.currentTimeMillis();
        final StringBuilder fullResponse = new StringBuilder();
        final boolean[] stopped = {false};
        final int[] tokenCount = {0};

        llmw.send(prompt, msg -> {
            runOnUiThread(() -> {
                try {
                    if (stopped[0]) return;

                    tokenCount[0]++;

                    if (tokenCount[0] > maxTokens) {
                        stopped[0] = true;
                        Log.d("Generation", "Max tokens reached: " + maxTokens);
                        JsonHelper.saveMessages(AiActivity.this, prefs.getUsername(), currentChat, messages);
                        return;
                    }

                    String currentFull = fullResponse.toString() + msg;
                    for (String stopToken : STOP_TOKENS) {
                        if (currentFull.toLowerCase().contains(stopToken.toLowerCase())) {
                            Log.d("Generation", "Stop token detected: " + stopToken);
                            stopped[0] = true;

                            String cleaned = currentFull.substring(0,
                                    currentFull.toLowerCase().indexOf(stopToken.toLowerCase()));
                            if (!cleaned.equals(fullResponse.toString())) {
                                Message aiMessage = messages.get(aiMessagePosition);
                                aiMessage.setContent(cleaned);
                                messageAdapter.notifyItemChanged(aiMessagePosition);
                            }

                            speak(cleaned);
                            JsonHelper.saveMessages(AiActivity.this, prefs.getUsername(), currentChat, messages);
                            return;
                        }
                    }

                    fullResponse.append(msg);

                    Message aiMessage = messages.get(aiMessagePosition);
                    aiMessage.setContent(fullResponse.toString());
                    messageAdapter.notifyItemChanged(aiMessagePosition);
                    messageList.scrollToPosition(aiMessagePosition);

                    if (fullResponse.length() <= msg.length()) {
                        long firstTokenTime = System.currentTimeMillis() - startTime;
                        Log.d("Performance", "First token in: " + firstTokenTime + "ms");
                    }

                    if (fullResponse.length() % 50 == 0) {
                        JsonHelper.saveMessages(AiActivity.this, prefs.getUsername(), currentChat, messages);
                    }

                } catch (Exception e) {
                    Log.e("AiActivity", "Error processing token", e);
                }
            });
        });

        messageList.postDelayed(() -> {
            JsonHelper.saveMessages(this, prefs.getUsername(), currentChat, messages);

            if (!textToSpeech.isSpeaking() && !messages.isEmpty()) {
                Message lastMsg = messages.get(messages.size() - 1);
                if (lastMsg.getSender() == 1) {
                    speak(lastMsg.getContent());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            Log.d("Performance", "Total generation time: " + totalTime + "ms for " + fullResponse.length() + " chars");
        }, 2000);
    }

    private void autoLoadModel() {
        String savedFilename = prefs.getCurrentModelFilename();
        if (savedFilename == null || savedFilename.isEmpty()) return;

        File modelFile = new File(getFilesDir(), savedFilename);

        if (modelFile.exists()) {
            runOnUiThread(() -> {
                ETinput.setHint("Loading model...");
                ETinput.setEnabled(false);
                BTNsend.setEnabled(false);
            });

            new Thread(() -> {
                try {
                    LLMW.Companion.getInstance(modelFile.getAbsolutePath(), prefs.getContextSize());
                    runOnUiThread(() -> {
                        refreshModelState();
                        Toast.makeText(AiActivity.this, "Model Loaded", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    Log.e("AiActivity", "Model load failed", e);
                    boolean isCorrupt = e.getMessage() != null &&
                            (e.getMessage().contains("file bounds") ||
                                    e.getMessage().contains("corrupted") ||
                                    e.getMessage().contains("failed to load"));

                    if (isCorrupt) {
                        if (modelFile.delete()) {
                            Log.d("AiActivity", "Corrupt file deleted");
                        }
                        prefs.setCurrentModel(null);
                        runOnUiThread(() -> {
                            refreshModelState();
                            new AlertDialog.Builder(AiActivity.this)
                                    .setTitle("Model Corrupted")
                                    .setMessage("The model file was incomplete or corrupted. It has been deleted. Please go to Settings and download it again.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    }
                }
            }).start();
        }
    }

    // ====== Chat Management ======

    private void startNewChat() {
        WelcomeText.setVisibility(View.GONE);
        TVguest.setVisibility(View.GONE);
        isInChat = true;
        Chat newChat = new Chat("Chat " + (chats.size() + 1), chats.size() + 1, null);
        chats.add(newChat);
        JsonHelper.saveChats(this, prefs.getUsername(), chats);
        chatAdapter.notifyItemInserted(chats.size() - 1);
        currentChat = newChat.getId();
        messages.clear();
        messageCount = 0;
        messageAdapter.notifyDataSetChanged();
    }

    private void createNewChat() {
        int maxId = 0;
        for (Chat c : chats) {
            if (c.getId() > maxId) maxId = c.getId();
        }
        int newId = maxId + 1;
        Chat newChat = new Chat("Chat " + newId, newId, null);
        chats.add(newChat);
        JsonHelper.saveChats(this, prefs.getUsername(), chats);
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
            if (chatList != null && !chats.isEmpty()) {
                chatList.smoothScrollToPosition(chats.size() - 1);
            }
        }
        loadChat(newChat);
    }

    private void loadChat(Chat chat) {
        currentChat = chat.getId();
        WelcomeText.setVisibility(View.GONE);
        TVguest.setVisibility(View.GONE);
        isInChat = true;
        messages.clear();
        messages.addAll(JsonHelper.loadMessages(this, prefs.getUsername(), chat.getId()));
        messageCount = messages.size() / 2;
        messageAdapter.notifyDataSetChanged();
        drawerLayout.closeDrawer(GravityCompat.START);

        if (!messages.isEmpty()) {
            messageList.scrollToPosition(messages.size() - 1);
        }
    }

    private void showDeleteChatDialog(Chat chat) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Delete \"" + chat.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int chatIdToDelete = chat.getId();
                    int index = chats.indexOf(chat);
                    if (index != -1) {
                        JsonHelper.removeChat(this, prefs.getUsername(), chatIdToDelete);
                        chats.remove(index);
                        chatAdapter.notifyItemRemoved(index);
                        if (currentChat == chatIdToDelete) {
                            messages.clear();
                            messageAdapter.notifyDataSetChanged();
                            isInChat = false;
                            WelcomeText.setVisibility(View.VISIBLE);
                            TVguest.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDebugOptions() {
        String[] options = {
                "Test with simple math",
                "Test with creative writing",
                "Clear conversation memory",
                "Show token stats",
                "Reset AI settings"
        };

        new AlertDialog.Builder(this)
                .setTitle("Debug Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            ETinput.setText("What is 15 + 27?");
                            BTNsend.performClick();
                            break;
                        case 1:
                            ETinput.setText("Write a haiku about programming");
                            BTNsend.performClick();
                            break;
                        case 2:
                            messages.clear();
                            messageAdapter.notifyDataSetChanged();
                            messageCount = 0;
                            Toast.makeText(this, "Conversation cleared", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            showTokenStats();
                            break;
                        case 4:
                            prefs.resetToDefaults();
                            Toast.makeText(this, "AI settings reset to defaults", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showTokenStats() {
        int totalChars = 0;
        for (Message m : messages) {
            totalChars += m.getContent().length();
        }
        int estimatedTokens = totalChars / 4;

        String stats = String.format(
                "Messages: %d\n" +
                        "Total chars: %d\n" +
                        "Estimated tokens: %d\n" +
                        "Context size: %d\n" +
                        "Max response: %d\n" +
                        "Quality mode: %s\n" +
                        "Context messages: %d",
                messages.size(),
                totalChars,
                estimatedTokens,
                prefs.getContextSize(),
                prefs.getMaxResponseTokens(),
                prefs.getQualityMode(),
                prefs.getMaxContextMessages()
        );

        new AlertDialog.Builder(this)
                .setTitle("Token Statistics")
                .setMessage(stats)
                .setPositiveButton("OK", null)
                .show();
    }

    // ====== Multimedia & TTS ======

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, REQUEST_VOICE_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        if (!isTtsReady || !prefs.isTtsEnabled() || text == null || text.isEmpty()) return;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        } else {
                            openCamera();
                        }
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        }
    }

    private void loadProfileImageFromDb() {
        if (drawerProfileImage == null) return;

        byte[] profilePicBytes = userDbHelper.getProfilePicture(prefs.getUsername());

        if (profilePicBytes != null && profilePicBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(profilePicBytes, 0, profilePicBytes.length);
            drawerProfileImage.setImageBitmap(bitmap);
        } else {
            drawerProfileImage.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    private Uri saveProfileImageToInternalStorage(Bitmap bitmap) {
        File file = new File(getFilesDir(), "profile.jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ====== Settings & Navigation ======

    private void showCustomSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        EditText maxTokensEdit = new EditText(this);
        maxTokensEdit.setHint("Max response tokens (current: " + prefs.getMaxResponseTokens() + ")");
        maxTokensEdit.setText(String.valueOf(prefs.getMaxResponseTokens()));
        maxTokensEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        EditText contextEdit = new EditText(this);
        contextEdit.setHint("Context messages (current: " + prefs.getMaxContextMessages() + ")");
        contextEdit.setText(String.valueOf(prefs.getMaxContextMessages()));
        contextEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        EditText tempEdit = new EditText(this);
        tempEdit.setHint("Temperature 0.1-1.0 (current: " + prefs.getTemperature() + ")");
        tempEdit.setText(String.valueOf(prefs.getTemperature()));
        tempEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView label1 = new TextView(this);
        label1.setText("Max Response Tokens (50-1024):");
        label1.setTextSize(14);
        layout.addView(label1);
        layout.addView(maxTokensEdit);

        TextView label2 = new TextView(this);
        label2.setText("\nContext Messages to Include (0-20):");
        label2.setTextSize(14);
        layout.addView(label2);
        layout.addView(contextEdit);

        TextView label3 = new TextView(this);
        label3.setText("\nTemperature (0.1=focused, 1.0=creative):");
        label3.setTextSize(14);
        layout.addView(label3);
        layout.addView(tempEdit);

        TextView infoText = new TextView(this);
        infoText.setText("\nNote: Lower temperature = more consistent\nHigher temperature = more creative");
        infoText.setTextSize(12);
        infoText.setTextColor(0xFF888888);
        layout.addView(infoText);

        builder.setView(layout)
                .setTitle("Custom AI Settings")
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int maxTokens = Integer.parseInt(maxTokensEdit.getText().toString());
                        int contextMsgs = Integer.parseInt(contextEdit.getText().toString());
                        float temperature = Float.parseFloat(tempEdit.getText().toString());

                        if (maxTokens < 50) maxTokens = 50;
                        if (maxTokens > 1024) maxTokens = 1024;
                        if (contextMsgs < 0) contextMsgs = 0;
                        if (contextMsgs > 20) contextMsgs = 20;
                        if (temperature < 0.1f) temperature = 0.1f;
                        if (temperature > 1.0f) temperature = 1.0f;

                        prefs.setMaxResponseTokens(maxTokens);
                        prefs.setMaxContextMessages(contextMsgs);
                        prefs.setTemperature(temperature);

                        Toast.makeText(this,
                                "Settings saved:\nTokens: " + maxTokens +
                                        "\nContext: " + contextMsgs +
                                        "\nTemp: " + String.format("%.1f", temperature),
                                Toast.LENGTH_LONG).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid values entered", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Defaults", (dialog, which) -> {
                    prefs.resetToDefaults();
                    Toast.makeText(this, "Reset to default settings", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAIConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String[] modes = {"Fast Mode", "Balanced Mode", "Quality Mode", "Custom Settings"};

        builder.setTitle("AI Configuration")
                .setItems(modes, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            prefs.applyFastMode();
                            Toast.makeText(this, "Fast mode applied - Quick responses, less context", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            prefs.applyBalancedMode();
                            Toast.makeText(this, "Balanced mode applied - Good performance and quality", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            prefs.applyQualityMode();
                            Toast.makeText(this, "Quality mode applied - Best responses, more context", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            showCustomSettingsDialog();
                            break;
                    }
                })
                .show();
    }

    private void logOut() {
        prefs.clearCardensials();
        if (llmw != null) {
            LLMW.Companion.unloadModel();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}