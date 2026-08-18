/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 */
package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.RegistrationDateEstimator;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;

import java.util.ArrayList;
import java.util.Date;

/**
 * Shows a preview of how the current account's own profile looks (the same info
 * other users would see), plus a few extra bits that don't fit anywhere else in
 * the standard profile screen: the raw Telegram id, a rough estimated
 * registration date (Telegram does not expose the real one via this API layer),
 * the channels/groups the user owns or administers, and the music files saved
 * in "Saved Messages".
 *
 * Note on scope: this app targets an older TL layer that does not have fields
 * for a "personal channel" or "profile music" (features that exist only in
 * newer official Telegram layers) - so those two are intentionally not shown
 * here.
 */
public class MyProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private final long selfUserId;
    private TLRPC.UserFull userFull;

    private final ArrayList<TLRPC.Chat> ownedChats = new ArrayList<>();

    private final SharedMediaLayout.SharedMediaData musicData = new SharedMediaLayout.SharedMediaData();
    private boolean musicLoading = true;
    private boolean musicRequested;

    private int rowCount;
    private int headerRow;
    private int infoHeaderRow;
    private int phoneRow;
    private int usernameRow;
    private int bioRow;
    private int infoSectionRow;
    private int accountHeaderRow;
    private int idRow;
    private int regDateRow;
    private int regDateInfoRow;
    private int accountSectionRow;
    private int channelsHeaderRow;
    private int channelsEmptyRow;
    private int channelsStartRow;
    private int channelsEndRow;
    private int channelsSectionRow;
    private int musicHeaderRow;
    private int musicEmptyRow;
    private int musicStartRow;
    private int musicEndRow;
    private int bottomPaddingRow;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_HEADER_CELL = 1;
    private static final int VIEW_TYPE_TEXT_DETAIL = 2;
    private static final int VIEW_TYPE_ABOUT_LINK = 3;
    private static final int VIEW_TYPE_SHADOW = 4;
    private static final int VIEW_TYPE_INFO = 5;
    private static final int VIEW_TYPE_CHAT = 6;
    private static final int VIEW_TYPE_MUSIC = 7;
    private static final int VIEW_TYPE_BOTTOM_PADDING = 8;

    public MyProfileActivity() {
        super();
        selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        getNotificationCenter().addObserver(this, NotificationCenter.userInfoDidLoad);
        getNotificationCenter().addObserver(this, NotificationCenter.mediaDidLoad);

        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        userFull = getMessagesController().getUserFull(selfUserId);
        if (userFull == null && user != null) {
            getMessagesController().loadUserInfo(user, false, getClassGuid());
        }

        loadOwnedChats();
        loadMusic();

        updateRows();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.userInfoDidLoad);
        getNotificationCenter().removeObserver(this, NotificationCenter.mediaDidLoad);
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userInfoDidLoad) {
            long uid = (Long) args[0];
            if (uid == selfUserId) {
                userFull = (TLRPC.UserFull) args[1];
                updateRows();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.mediaDidLoad) {
            long uid = (Long) args[0];
            int guid = (Integer) args[3];
            int type = (Integer) args[4];
            if (uid == selfUserId && guid == getClassGuid() && type == MediaDataController.MEDIA_MUSIC) {
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[2];
                for (int i = 0; i < arr.size(); i++) {
                    musicData.addMessage(arr.get(i), 0, false, false);
                }
                musicLoading = false;
                updateRows();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void loadOwnedChats() {
        ownedChats.clear();
        ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getAllDialogs();
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (!DialogObject.isChatDialog(dialog.id)) {
                continue;
            }
            TLRPC.Chat chat = getMessagesController().getChat(-dialog.id);
            if (chat == null || chat.migrated_to != null || chat.left || chat.kicked) {
                continue;
            }
            if (ChatObject.hasAdminRights(chat)) {
                ownedChats.add(chat);
            }
        }
    }

    private void loadMusic() {
        if (musicRequested) {
            return;
        }
        musicRequested = true;
        musicData.setMaxId(0, Integer.MAX_VALUE);
        getMediaDataController().loadMedia(selfUserId, 40, 0, 0, MediaDataController.MEDIA_MUSIC, 0, 0, getClassGuid(), 0);
    }

    private void updateRows() {
        rowCount = 0;
        headerRow = rowCount++;

        infoHeaderRow = rowCount++;
        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        phoneRow = (user != null && !TextUtils.isEmpty(user.phone)) ? rowCount++ : -1;
        usernameRow = (user != null && !TextUtils.isEmpty(UserObject.getPublicUsername(user))) ? rowCount++ : -1;
        bioRow = rowCount++;
        infoSectionRow = rowCount++;

        accountHeaderRow = rowCount++;
        idRow = rowCount++;
        regDateRow = rowCount++;
        regDateInfoRow = rowCount++;
        accountSectionRow = rowCount++;

        channelsHeaderRow = rowCount++;
        if (ownedChats.isEmpty()) {
            channelsEmptyRow = rowCount++;
            channelsStartRow = -1;
            channelsEndRow = -1;
        } else {
            channelsEmptyRow = -1;
            channelsStartRow = rowCount;
            rowCount += ownedChats.size();
            channelsEndRow = rowCount;
        }
        channelsSectionRow = rowCount++;

        musicHeaderRow = rowCount++;
        if (musicLoading || musicData.messages.isEmpty()) {
            musicEmptyRow = rowCount++;
            musicStartRow = -1;
            musicEndRow = -1;
        } else {
            musicEmptyRow = -1;
            musicStartRow = rowCount;
            rowCount += musicData.messages.size();
            musicEndRow = rowCount;
        }

        bottomPaddingRow = rowCount++;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("MyProfile", R.string.MyProfile));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == idRow) {
                TextDetailCell cell = (TextDetailCell) view;
                if (cell.getText() != null) {
                    AndroidUtilities.addToClipboard(cell.getText());
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied), getResourceProvider()).show();
                }
            } else if (position >= channelsStartRow && channelsStartRow >= 0 && position < channelsEndRow) {
                TLRPC.Chat chat = ownedChats.get(position - channelsStartRow);
                Bundle args = new Bundle();
                args.putLong("chat_id", chat.id);
                presentFragment(new ChatActivity(args));
            } else if (position >= musicStartRow && musicStartRow >= 0 && position < musicEndRow) {
                if (view instanceof SharedAudioCell) {
                    ((SharedAudioCell) view).didPressedButton();
                }
            }
        });

        return fragmentView;
    }

    private class ProfileHeaderView extends FrameLayout {

        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final BackupImageView avatarView;
        private final TextView nameView;
        private final TextView badgeView;

        public ProfileHeaderView(Context context) {
            super(context);
            setWillNotDraw(false);

            FrameLayout avatarContainer = new FrameLayout(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    rect.set(0, 0, getWidth(), getHeight());
                    glowPaint.setStyle(Paint.Style.STROKE);
                    glowPaint.setStrokeWidth(AndroidUtilities.dp(2.5f));
                    glowPaint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                            0xFF00E5FF, 0xFF7C4DFF, Shader.TileMode.CLAMP));
                    canvas.drawOval(rect, glowPaint);
                }
            };
            avatarContainer.setWillNotDraw(false);

            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(46));
            avatarContainer.addView(avatarView, LayoutHelper.createFrame(92, 92, Gravity.CENTER));
            addView(avatarContainer, LayoutHelper.createFrame(98, 98, Gravity.CENTER | Gravity.TOP, 0, 22, 0, 0));

            nameView = new TextView(context);
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            nameView.setGravity(Gravity.CENTER);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP, 24, 130, 24, 0));

            badgeView = new TextView(context) {
                private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                private final RectF bgRect = new RectF();

                @Override
                protected void onDraw(Canvas canvas) {
                    bgRect.set(0, 0, getWidth(), getHeight());
                    bgPaint.setStyle(Paint.Style.FILL);
                    bgPaint.setColor(0x2600E5FF);
                    canvas.drawRoundRect(bgRect, AndroidUtilities.dp(12), AndroidUtilities.dp(12), bgPaint);
                    bgPaint.setStyle(Paint.Style.STROKE);
                    bgPaint.setStrokeWidth(AndroidUtilities.dp(1));
                    bgPaint.setColor(0x8000E5FF);
                    canvas.drawRoundRect(bgRect, AndroidUtilities.dp(12), AndroidUtilities.dp(12), bgPaint);
                    super.onDraw(canvas);
                }
            };
            badgeView.setTextColor(0xFF00E5FF);
            badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badgeView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            badgeView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(3), AndroidUtilities.dp(10), AndroidUtilities.dp(3));
            badgeView.setGravity(Gravity.CENTER);
            addView(badgeView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER | Gravity.TOP, 0, 162, 0, 0));
        }

        public void bind(TLRPC.User user) {
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(user);
            avatarView.setForUserOrChat(user, avatarDrawable);
            nameView.setText(UserObject.getUserName(user));
            String username = user != null ? UserObject.getPublicUsername(user) : null;
            badgeView.setText(!TextUtils.isEmpty(username) ? "@" + username : LocaleController.getString("MyProfileSubtitle", R.string.MyProfileSubtitle));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(198), MeasureSpec.EXACTLY));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == idRow ||
                    (position >= channelsStartRow && channelsStartRow >= 0 && position < channelsEndRow) ||
                    (position >= musicStartRow && musicStartRow >= 0 && position < musicEndRow);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    view = new ProfileHeaderView(mContext);
                    break;
                case VIEW_TYPE_HEADER_CELL:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_TEXT_DETAIL:
                    view = new TextDetailCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_ABOUT_LINK:
                    view = new AboutLinkCell(mContext, MyProfileActivity.this);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case VIEW_TYPE_INFO:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case VIEW_TYPE_CHAT:
                    view = new ProfileSearchCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_MUSIC:
                    SharedAudioCell audioCell = new SharedAudioCell(mContext) {
                        @Override
                        protected boolean needPlayMessage(MessageObject messageObject) {
                            if (messageObject.isVoice() || messageObject.isRoundVideo()) {
                                boolean result = MediaController.getInstance().playMessage(messageObject);
                                MediaController.getInstance().setVoiceMessagesPlaylist(result ? musicData.messages : null, false);
                                return result;
                            } else if (messageObject.isMusic()) {
                                return MediaController.getInstance().setPlaylist(musicData.messages, messageObject, 0);
                            }
                            return false;
                        }
                    };
                    audioCell.initStreamingIcons();
                    view = audioCell;
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_BOTTOM_PADDING:
                default:
                    view = new View(mContext) {
                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(16), MeasureSpec.EXACTLY));
                        }
                    };
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HEADER:
                    ((ProfileHeaderView) holder.itemView).bind(user);
                    break;
                case VIEW_TYPE_HEADER_CELL: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == infoHeaderRow) {
                        headerCell.setText(LocaleController.getString("Info", R.string.Info));
                    } else if (position == accountHeaderRow) {
                        headerCell.setText(LocaleController.getString("Account", R.string.Account));
                    } else if (position == channelsHeaderRow) {
                        headerCell.setText(LocaleController.getString("MyProfileChannelsSection", R.string.MyProfileChannelsSection));
                    } else if (position == musicHeaderRow) {
                        String value = musicLoading ? LocaleController.getString("MyProfileMusicSection", R.string.MyProfileMusicSection)
                                : LocaleController.getString("MyProfileMusicSection", R.string.MyProfileMusicSection) + (musicData.messages.isEmpty() ? "" : " (" + LocaleController.formatPluralString("MusicFiles", musicData.messages.size()) + ")");
                        headerCell.setText(value);
                    }
                    break;
                }
                case VIEW_TYPE_TEXT_DETAIL: {
                    TextDetailCell cell = (TextDetailCell) holder.itemView;
                    if (position == phoneRow) {
                        String value = user != null && !TextUtils.isEmpty(user.phone) ? PhoneFormat.getInstance().format("+" + user.phone) : "";
                        cell.setTextAndValue(value, LocaleController.getString("PhoneMobile", R.string.PhoneMobile), usernameRow != -1 || bioRow != -1);
                    } else if (position == usernameRow) {
                        String username = user != null ? UserObject.getPublicUsername(user) : null;
                        cell.setTextAndValue(username != null ? "@" + username : "", LocaleController.getString("Username", R.string.Username), true);
                    } else if (position == idRow) {
                        cell.setTextAndValue(String.valueOf(selfUserId), LocaleController.getString("MyProfileTelegramId", R.string.MyProfileTelegramId), true);
                    } else if (position == regDateRow) {
                        long ms = RegistrationDateEstimator.estimateRegistrationDateMs(selfUserId);
                        String value = ms > 0 ? LocaleController.formatString("MyProfileRegDateApprox", R.string.MyProfileRegDateApprox, LocaleController.getInstance().formatterMonthYear.format(new Date(ms))) : "—";
                        cell.setTextAndValue(value, LocaleController.getString("MyProfileRegDate", R.string.MyProfileRegDate), false);
                    }
                    break;
                }
                case VIEW_TYPE_ABOUT_LINK: {
                    AboutLinkCell cell = (AboutLinkCell) holder.itemView;
                    String about = userFull != null ? userFull.about : null;
                    if (TextUtils.isEmpty(about)) {
                        about = LocaleController.getString("Loading", R.string.Loading);
                    }
                    cell.setTextAndValue(about, LocaleController.getString("UserBio", R.string.UserBio), true, false);
                    break;
                }
                case VIEW_TYPE_INFO: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == regDateInfoRow) {
                        cell.setText(LocaleController.getString("MyProfileRegDateInfo", R.string.MyProfileRegDateInfo));
                    } else if (position == channelsEmptyRow) {
                        cell.setText(LocaleController.getString("MyProfileNoChannels", R.string.MyProfileNoChannels));
                    } else if (position == musicEmptyRow) {
                        cell.setText(musicLoading ? LocaleController.getString("Loading", R.string.Loading) : LocaleController.getString("MyProfileNoMusic", R.string.MyProfileNoMusic));
                    }
                    break;
                }
                case VIEW_TYPE_CHAT: {
                    ProfileSearchCell cell = (ProfileSearchCell) holder.itemView;
                    TLRPC.Chat chat = ownedChats.get(position - channelsStartRow);
                    boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
                    String role = chat.creator ? LocaleController.getString("ChannelCreator", R.string.ChannelCreator) : LocaleController.getString("ChannelAdmin", R.string.ChannelAdmin);
                    String type = LocaleController.getString(isChannel ? "AccDescrChannel" : "AccDescrGroup", isChannel ? R.string.AccDescrChannel : R.string.AccDescrGroup);
                    cell.setData(chat, null, null, type + " • " + role, false, false);
                    cell.useSeparator = position != channelsEndRow - 1;
                    break;
                }
                case VIEW_TYPE_MUSIC: {
                    SharedAudioCell cell = (SharedAudioCell) holder.itemView;
                    MessageObject messageObject = musicData.messages.get(position - musicStartRow);
                    cell.setMessageObject(messageObject, position != musicEndRow - 1);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerRow) {
                return VIEW_TYPE_HEADER;
            } else if (position == infoHeaderRow || position == accountHeaderRow || position == channelsHeaderRow || position == musicHeaderRow) {
                return VIEW_TYPE_HEADER_CELL;
            } else if (position == phoneRow || position == usernameRow || position == idRow || position == regDateRow) {
                return VIEW_TYPE_TEXT_DETAIL;
            } else if (position == bioRow) {
                return VIEW_TYPE_ABOUT_LINK;
            } else if (position == infoSectionRow || position == accountSectionRow || position == channelsSectionRow) {
                return VIEW_TYPE_SHADOW;
            } else if (position == regDateInfoRow || position == channelsEmptyRow || position == musicEmptyRow) {
                return VIEW_TYPE_INFO;
            } else if (position >= channelsStartRow && channelsStartRow >= 0 && position < channelsEndRow) {
                return VIEW_TYPE_CHAT;
            } else if (position >= musicStartRow && musicStartRow >= 0 && position < musicEndRow) {
                return VIEW_TYPE_MUSIC;
            } else {
                return VIEW_TYPE_BOTTOM_PADDING;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> descriptions = new ArrayList<>();
        descriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        return descriptions;
    }
}
