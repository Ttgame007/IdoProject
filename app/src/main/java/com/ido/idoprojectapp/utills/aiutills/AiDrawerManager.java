package com.ido.idoprojectapp.utills.aiutills;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.db.HelperUserDB;
import com.ido.idoprojectapp.deta.model.Chat;
import com.ido.idoprojectapp.deta.prefs.PrefsHelper;
import com.ido.idoprojectapp.ui.adapters.ChatAdapter;
import com.ido.idoprojectapp.utills.helpers.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class AiDrawerManager {
    private final Activity activity;
    private final DrawerLayout drawerLayout;
    private final RecyclerView chatList;
    private final PrefsHelper prefs;
    private final HelperUserDB userDb;
    private ShapeableImageView drawerProfileImage;
    private Button profileBtn;

    public interface DrawerActionListener {
        void onNewChat();
        void onModelSettings();
        void onLogout();
        void onSettings();
        void onChatSelected(Chat chat);
        void onChatDelete(Chat chat);
        void onProfileImageClick();
    }

    public AiDrawerManager(Activity activity, View rootView, PrefsHelper prefs, HelperUserDB userDb, DrawerActionListener listener) {
        this.activity = activity;
        this.prefs = prefs;
        this.userDb = userDb;
        this.drawerLayout = rootView.findViewById(R.id.main);
        this.chatList = rootView.findViewById(R.id.chatList);

        initViews(rootView, listener);
        setupChatList(listener);
        updateProfileDisplay();
    }

    private void initViews(View root, DrawerActionListener listener) {
        drawerProfileImage = root.findViewById(R.id.drawerProfileImage);
        profileBtn = root.findViewById(R.id.profileNameBtn);
        Button newChatBtn = root.findViewById(R.id.newChatBtn);
        Button modelSettingsBtn = root.findViewById(R.id.modelSettingsBtn);
        ImageButton logoutIcon = root.findViewById(R.id.logoutIcon);
        ImageButton settingIcon = root.findViewById(R.id.settingIcon);
        ImageButton menuIcon = root.findViewById(R.id.menuIcon);

        menuIcon.setOnClickListener(v -> toggleDrawer());
        newChatBtn.setOnClickListener(v -> listener.onNewChat());
        modelSettingsBtn.setOnClickListener(v -> listener.onModelSettings());
        logoutIcon.setOnClickListener(v -> listener.onLogout());
        settingIcon.setOnClickListener(v -> listener.onSettings());

        settingIcon.setOnLongClickListener(v -> {
            listener.onSettings();
            return true;
        });

        drawerProfileImage.setOnClickListener(v -> listener.onProfileImageClick());
        profileBtn.setOnClickListener(v -> listener.onSettings());
    }

    private void setupChatList(DrawerActionListener listener) {
        chatList.setLayoutManager(new LinearLayoutManager(activity));
        chatList.setAdapter(new ChatAdapter(new ArrayList<>(), new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(Chat chat) {
                listener.onChatSelected(chat);
                closeDrawer();
            }
            @Override
            public void onChatLongClick(Chat chat) {
                listener.onChatDelete(chat);
            }
        }));
    }

    public void updateChats(List<Chat> chats, ChatAdapter.OnChatClickListener listener) {
        chatList.setAdapter(new ChatAdapter(chats, listener));
    }

    public void updateProfileDisplay() {
        if (profileBtn != null) profileBtn.setText(prefs.getUsername());
        if (drawerProfileImage != null) {
            byte[] bytes = userDb.getProfilePicture(prefs.getUsername());
            if (bytes != null) {
                drawerProfileImage.setImageBitmap(UIHelper.bytesToBitmap(bytes));
            } else {
                drawerProfileImage.setImageResource(R.drawable.ic_default_avatar);
            }
        }
    }

    public void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else drawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
    }
}