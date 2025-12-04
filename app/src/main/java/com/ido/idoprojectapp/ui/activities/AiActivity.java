package com.ido.idoprojectapp.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.db.HelperUserDB;
import com.ido.idoprojectapp.deta.model.Chat;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;
import com.ido.idoprojectapp.services.LLMW;
import com.ido.idoprojectapp.services.TTSHelper;
import com.ido.idoprojectapp.ui.adapters.ChatAdapter;
import com.ido.idoprojectapp.ui.adapters.MessageAdapter;
import com.ido.idoprojectapp.ui.viewmodels.ChatViewModel;
import com.ido.idoprojectapp.utills.aiutills.AiDrawerManager;
import com.ido.idoprojectapp.utills.helpers.AnimationHelper;
import com.ido.idoprojectapp.utills.helpers.CustomDialogHelper;
import com.ido.idoprojectapp.utills.helpers.MediaActionHelper;
import com.ido.idoprojectapp.utills.helpers.UIHelper;

import java.io.IOException;
import java.util.ArrayList;

public class AiActivity extends AppCompatActivity implements AiDrawerManager.DrawerActionListener {

    private ChatViewModel viewModel;
    private TTSHelper ttsHelper;
    private PrefsHelper prefs;
    private HelperUserDB userDb;
    private AiDrawerManager drawerManager;
    private MediaActionHelper mediaHelper;
    private AnimationHelper animationHelper;

    private Button btnSend;
    private EditText etInput;
    private TextView welcomeText, tvGuest;
    private RecyclerView messageList;
    private MessageAdapter messageAdapter;

    private int currentChatId = -1;
    private boolean isInChat = false;
    private static final int REQUEST_MODEL_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PrefsHelper(this);
        AppCompatDelegate.setDefaultNightMode(prefs.isNightModeEnabled() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_ai);

        userDb = new HelperUserDB(this);
        ttsHelper = new TTSHelper(this);
        mediaHelper = new MediaActionHelper(this);
        animationHelper = new AnimationHelper();
        drawerManager = new AiDrawerManager(this, findViewById(R.id.main), prefs, userDb, this);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        initViews();
        setupObservers();

        viewModel.initializeModel(prefs.getCurrentModelFilename());
        viewModel.loadChats();
    }

    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        tvGuest = findViewById(R.id.TVguest);
        etInput = findViewById(R.id.ETinput);
        btnSend = findViewById(R.id.BTNsend);
        messageList = findViewById(R.id.messageList);

        tvGuest.setText(prefs.getUsername());

        messageAdapter = new MessageAdapter(new ArrayList<>());
        messageList.setAdapter(messageAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.BTNmic).setOnClickListener(v -> mediaHelper.startVoiceInput());

        btnSend.setOnClickListener(v -> sendMessage());
        btnSend.setOnLongClickListener(v -> {
            showDebugOptions();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshModelState();
        drawerManager.updateProfileDisplay();
        viewModel.loadChats();
    }

    @Override
    protected void onDestroy() {
        ttsHelper.shutdown();
        super.onDestroy();
    }

    // <=== ViewModel & Logic ===>

    private void setupObservers() {
        viewModel.chats.observe(this, chats -> {
            drawerManager.updateChats(chats, new ChatAdapter.OnChatClickListener() {
                @Override public void onChatClick(Chat chat) { onChatSelected(chat); drawerManager.closeDrawer(); }
                @Override public void onChatLongClick(Chat chat) { onChatDelete(chat); }
            });

            if (chats.isEmpty()) resetUI();
        });

        viewModel.messages.observe(this, messages -> {
            messageAdapter = new MessageAdapter(messages);
            messageList.setAdapter(messageAdapter);
            if (!messages.isEmpty()) messageList.scrollToPosition(messages.size() - 1);
        });

        viewModel.isGenerating.observe(this, isGen -> {
            animationHelper.toggleThinkingAnimation(etInput, isGen);
            btnSend.setEnabled(!isGen);
            etInput.setEnabled(!isGen);
        });

        viewModel.streamToken.observe(this, token -> {
            if (messageAdapter.getItemCount() > 0) {
                messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                messageList.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });

        viewModel.finishedResponse.observe(this, response -> ttsHelper.speak(response));
    }

    private void sendMessage() {
        if (!viewModel.isModelLoaded()) {
            UIHelper.showError(this, btnSend, "No model loaded");
            return;
        }
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) return;

        if (!isInChat) startNewChatFromUI();

        etInput.setText("");
        etInput.clearFocus();
        viewModel.sendMessage(input, currentChatId);
    }

    private void startNewChatFromUI() {
        welcomeText.setVisibility(View.GONE);
        tvGuest.setVisibility(View.GONE);
        isInChat = true;
        viewModel.createNewChat();
        if (viewModel.chats.getValue() != null && !viewModel.chats.getValue().isEmpty()) {
            currentChatId = viewModel.chats.getValue().get(viewModel.chats.getValue().size() - 1).getId();
        }
    }

    private void resetUI() {
        isInChat = false;
        welcomeText.setVisibility(View.VISIBLE);
        tvGuest.setVisibility(View.VISIBLE);
        messageAdapter = new MessageAdapter(new ArrayList<>());
        messageList.setAdapter(messageAdapter);
    }

    private void refreshModelState() {
        if (viewModel.isModelLoaded()) {
            etInput.setHint("Type your message...");
            btnSend.setEnabled(true);
        } else {
            etInput.setHint("Download a model in settings");
            btnSend.setEnabled(false);
            String saved = prefs.getCurrentModelFilename();
            if (saved != null) viewModel.initializeModel(saved);
        }
    }

    // <=== Drawer Actions (Interface Implementation) ===>

    @Override
    public void onNewChat() {
        viewModel.createNewChat();
        startNewChatFromUI();
        drawerManager.closeDrawer();
    }

    @Override
    public void onModelSettings() {
        startActivityForResult(new Intent(this, ModelSettingsActivity.class), REQUEST_MODEL_SETTINGS);
    }

    @Override
    public void onLogout() {
        prefs.clearCardensials();
        LLMW.Companion.unloadModel();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onChatSelected(Chat chat) {
        currentChatId = chat.getId();
        isInChat = true;
        welcomeText.setVisibility(View.GONE);
        tvGuest.setVisibility(View.GONE);
        viewModel.loadMessages(chat.getId());
    }

    @Override
    public void onChatDelete(Chat chat) {
        CustomDialogHelper.showConfirmation(this, "Delete Chat", "Delete " + chat.getName() + "?", "Delete", "Cancel", () -> {
            viewModel.deleteChat(chat);
            if (currentChatId == chat.getId()) resetUI();
        });
    }

    @Override
    public void onProfileImageClick() {
        mediaHelper.showImageSourceDialog(mediaHelper::openCamera, mediaHelper::openGallery);
    }

    // <=== Activity Results ===>

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_MODEL_SETTINGS) {
            refreshModelState();
        } else if (requestCode == MediaActionHelper.REQUEST_VOICE_INPUT && data != null) {
            ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (res != null && !res.isEmpty()) etInput.setText(res.get(0));
        } else {
            handleImageResult(requestCode, data);
        }
    }

    private void handleImageResult(int requestCode, Intent data) {
        try {
            Bitmap bitmap = null;
            if (requestCode == MediaActionHelper.REQUEST_PICK_IMAGE && data != null) {
                bitmap = UIHelper.getBitmapFromUri(this, data.getData());
            } else if (requestCode == MediaActionHelper.REQUEST_CAPTURE_IMAGE && data != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (bitmap != null) {
                if (userDb.updateProfilePicture(prefs.getUsername(), UIHelper.bitmapToBytes(bitmap))) {
                    drawerManager.updateProfileDisplay();
                    UIHelper.showInfo(this, "Profile updated");
                }
            }
        } catch (IOException e) {
            UIHelper.showError(this, "Failed to load image");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MediaActionHelper.REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mediaHelper.openCamera();
        }
    }

    private void showDebugOptions() {
        CustomDialogHelper.showOptionsDialog(this, "Debug", new String[]{"Test Math", "Stats", "Config"}, i -> {
            if (i == 0) { etInput.setText("10 + 10?"); btnSend.performClick(); }
            else if (i == 1) UIHelper.showInfo(this, "Ctx: " + prefs.getContextSize());
            else if (i == 2) showAIConfigDialog();
        });
    }

    private void showAIConfigDialog() {
        String[] modes = {"Fast Mode", "Balanced Mode", "Quality Mode"};
        CustomDialogHelper.showOptionsDialog(this, "AI Configuration", modes, index -> {
            switch (index) {
                case 0: prefs.applyFastMode(); break;
                case 1: prefs.applyBalancedMode(); break;
                case 2: prefs.applyQualityMode(); break;
            }
            UIHelper.showInfo(this, "Mode Applied");
        });
    }
}