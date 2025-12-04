package com.ido.idoprojectapp.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.ido.idoprojectapp.deta.model.Chat;
import com.ido.idoprojectapp.deta.model.Message;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;
import com.ido.idoprojectapp.services.LLMW;
import com.ido.idoprojectapp.deta.db.JsonHelper;
import com.ido.idoprojectapp.utills.aiutills.PromptBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final PrefsHelper prefs;
    private LLMW llmw;
    private boolean stopGeneration = false;
    private StringBuilder fullResponseBuffer;

    public MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<Chat>> chats = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);
    public MutableLiveData<String> streamToken = new MutableLiveData<>(); // Updates while typing
    public MutableLiveData<String> finishedResponse = new MutableLiveData<>(); // Triggers TTS

    private static final List<String> STOP_TOKENS = Arrays.asList(
            "\nuser:", "\nUser:", "\nUSER:", "###", "</s>", "<|im_end|>"
    );

    public ChatViewModel(@NonNull Application application) {
        super(application);
        prefs = new PrefsHelper(application);
    }

    public void initializeModel(String savedFilename) {
        if (!LLMW.Companion.isLoaded() && savedFilename != null) {
            File modelFile = new File(getApplication().getFilesDir(), savedFilename);
            if (modelFile.exists()) {
                llmw = LLMW.Companion.getInstance(modelFile.getAbsolutePath(), prefs.getContextSize());
            }
        } else if (LLMW.Companion.isLoaded()) {
            llmw = LLMW.Companion.getInstance(null);
        }
    }

    public boolean isModelLoaded() {
        return LLMW.Companion.isLoaded();
    }

    public void loadChats() {
        List<Chat> loadedChats = JsonHelper.loadChats(getApplication(), prefs.getUsername());
        if (loadedChats == null) loadedChats = new ArrayList<>();
        chats.setValue(loadedChats);
    }

    public void loadMessages(int chatId) {
        List<Message> loadedMessages = JsonHelper.loadMessages(getApplication(), prefs.getUsername(), chatId);
        messages.setValue(loadedMessages);
    }

    public void createNewChat() {
        List<Chat> currentChats = chats.getValue();
        if (currentChats == null) currentChats = new ArrayList<>();

        int maxId = 0;
        for (Chat c : currentChats) if (c.getId() > maxId) maxId = c.getId();

        Chat newChat = new Chat("Chat " + (maxId + 1), maxId + 1, null);
        currentChats.add(newChat);
        JsonHelper.saveChats(getApplication(), prefs.getUsername(), currentChats);
        chats.setValue(currentChats);

        messages.setValue(new ArrayList<>());
    }

    public void deleteChat(Chat chat) {
        JsonHelper.removeChat(getApplication(), prefs.getUsername(), chat.getId());
        List<Chat> currentChats = chats.getValue();
        if (currentChats != null) {
            currentChats.remove(chat);
            chats.setValue(currentChats);
        }
    }

    public void sendMessage(String userText, int chatId) {
        List<Message> currentMsgs = messages.getValue();
        if (currentMsgs == null) currentMsgs = new ArrayList<>();

        currentMsgs.add(new Message(userText, 0));

        Message aiMsg = new Message("", 1);
        currentMsgs.add(aiMsg);

        messages.setValue(currentMsgs);
        isGenerating.setValue(true);

        String prompt = PromptBuilder.build(prefs, currentMsgs.subList(0, currentMsgs.size()-2), userText);

        generate(prompt, aiMsg, chatId, prefs.getMaxResponseTokens());
    }

    private void generate(String prompt, Message aiMsgObject, int chatId, int maxTokens) {
        fullResponseBuffer = new StringBuilder();
        stopGeneration = false;
        final int[] tokenCount = {0};

        llmw.send(prompt, token -> {
            if (stopGeneration) return;

            new Handler(Looper.getMainLooper()).post(() -> {
                tokenCount[0]++;

                // Safety Limit
                if (tokenCount[0] > maxTokens) {
                    stopGeneration = true;
                    finalizeGeneration(chatId);
                    return;
                }

                String currentFull = fullResponseBuffer.toString() + token;

                for (String stop : STOP_TOKENS) {
                    if (currentFull.toLowerCase().contains(stop.toLowerCase())) {
                        stopGeneration = true;
                        String cleaned = currentFull.substring(0, currentFull.toLowerCase().indexOf(stop.toLowerCase()));
                        aiMsgObject.setContent(cleaned);
                        finalizeGeneration(chatId);
                        return;
                    }
                }

                fullResponseBuffer.append(token);
                aiMsgObject.setContent(fullResponseBuffer.toString());

                streamToken.setValue(token);
            });
        }, prefs.getTemperature(), prefs.getTopK(), prefs.getTopP(), prefs.getRepeatPenalty());
    }

    private void finalizeGeneration(int chatId) {
        isGenerating.setValue(false);
        finishedResponse.setValue(fullResponseBuffer.toString()); // Triggers TTS
        JsonHelper.saveMessages(getApplication(), prefs.getUsername(), chatId, messages.getValue());
    }
}