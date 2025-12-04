package com.ido.idoprojectapp.utills.aiutills;

import com.ido.idoprojectapp.deta.model.Message;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;

import java.util.List;

public class PromptBuilder {
    public static String build(PrefsHelper prefs, List<Message> messages, String userInput) {
        StringBuilder prompt = new StringBuilder();
        int maxContextMessages = prefs.getMaxContextMessages();

        String currentSystemPrompt = prefs.getEffectiveSystemPrompt();

        prompt.append("### System\n");
        prompt.append(currentSystemPrompt).append("\n\n");

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
}