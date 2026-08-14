/*
 * Alternative login flow that authenticates an account slot with a Telegram Bot API
 * token (from @BotFather) instead of a phone number. See org.telegram.messenger.botapi
 * for the network/translation layer this feeds into.
 */

package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.botapi.BotApiClient;
import org.telegram.messenger.botapi.BotApiModels;
import org.telegram.messenger.botapi.BotApiPoller;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class LoginBotTokenActivity extends BaseFragment {

    private EditText tokenEditText;
    private TextView statusTextView;
    private TextView doneButton;
    private boolean loading;

    public LoginBotTokenActivity() {
        super();
    }

    public LoginBotTokenActivity(int account) {
        super();
        currentAccount = account;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Bot Token orqali kirish");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        scrollView.addView(container, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        TextView infoText = new TextView(context);
        infoText.setText("@BotFather orqali olingan bot tokenini kiriting. Bot yozishgan foydalanuvchilar va a'zo bo'lgan guruhlardagi xabarlar shu ilovada oddiy chat sifatida ko'rinadi.\n\nEslatma: bot birinchi bo'lib xabar yoza olmaydi — foydalanuvchi avval botga /start yozishi kerak.");
        infoText.setTextSize(14);
        infoText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        container.addView(infoText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        tokenEditText = new EditText(context);
        tokenEditText.setHint("123456789:ABC-DEF1234ghIkl...");
        tokenEditText.setSingleLine(true);
        tokenEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        tokenEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tokenEditText.setTextSize(16);
        tokenEditText.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tokenEditText.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        container.addView(tokenEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 16));

        statusTextView = new TextView(context);
        statusTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        statusTextView.setTextSize(13);
        container.addView(statusTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        doneButton = new TextView(context);
        doneButton.setText("Kirish");
        doneButton.setGravity(Gravity.CENTER);
        doneButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        doneButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        doneButton.setTextSize(15);
        doneButton.setOnClickListener(v -> onDoneClicked());
        container.addView(doneButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        return fragmentView;
    }

    private void onDoneClicked() {
        if (loading) {
            return;
        }
        String token = tokenEditText.getText().toString().trim();
        if (TextUtils.isEmpty(token) || !token.contains(":")) {
            statusTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
            statusTextView.setText("Token noto'g'ri formatda");
            return;
        }
        loading = true;
        doneButton.setAlpha(0.6f);
        statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusTextView.setText("Tekshirilmoqda...");

        BotApiClient client = new BotApiClient();
        Utilities.globalQueue.postRunnable(() -> {
            BotApiModels.User botUser;
            try {
                botUser = client.getMe(token);
            } catch (Exception e) {
                botUser = null;
            }
            BotApiModels.User finalBotUser = botUser;
            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                if (doneButton == null) {
                    return;
                }
                doneButton.setAlpha(1f);
                if (finalBotUser == null) {
                    statusTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                    statusTextView.setText("Token noto'g'ri yoki tarmoq xatosi");
                    return;
                }
                finishLogin(finalBotUser, token);
            });
        });
    }

    private void finishLogin(BotApiModels.User botUser, String token) {
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = botUser.id;
        user.first_name = !TextUtils.isEmpty(botUser.firstName) ? botUser.firstName : "Bot";
        user.last_name = botUser.lastName;
        user.username = botUser.username;
        user.bot = true;
        user.access_hash = 0;

        MessagesController.getInstance(currentAccount).cleanup();
        UserConfig.getInstance(currentAccount).clearConfig();
        MessagesController.getInstance(currentAccount).cleanup();
        UserConfig.getInstance(currentAccount).botApiMode = true;
        UserConfig.getInstance(currentAccount).botApiToken = token;
        UserConfig.getInstance(currentAccount).botApiUpdateOffset = 0;
        UserConfig.getInstance(currentAccount).setCurrentUser(user);
        UserConfig.getInstance(currentAccount).saveConfig(true);
        MessagesStorage.getInstance(currentAccount).cleanup(true);

        ArrayList<TLRPC.User> users = new ArrayList<>();
        users.add(user);
        MessagesStorage.getInstance(currentAccount).putUsersAndChats(users, null, true, true);
        MessagesController.getInstance(currentAccount).putUser(user, false);

        BotApiPoller.getInstance(currentAccount).start();

        if (getParentActivity() instanceof LaunchActivity) {
            int account = currentAccount;
            ((LaunchActivity) getParentActivity()).switchToAccount(account, false, obj -> new DialogsActivity(new Bundle()));
            finishFragment();
        }
    }
}
