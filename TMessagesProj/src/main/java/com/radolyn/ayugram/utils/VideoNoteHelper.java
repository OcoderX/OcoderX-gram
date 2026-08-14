package com.radolyn.ayugram.utils;

import android.media.MediaMetadataRetriever;
import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;

public class VideoNoteHelper {

    public static VideoEditedInfo createRoundVideoEditedInfo(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            return null;
        }

        int width = 0;
        int height = 0;
        long duration = 0;
        int rotation = 0;

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);

            String w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (w != null) {
                width = Integer.parseInt(w);
            }
            String h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (h != null) {
                height = Integer.parseInt(h);
            }
            String d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (d != null) {
                duration = Long.parseLong(d);
            }
            String r = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (r != null) {
                rotation = Utilities.parseInt(r);
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            try {
                if (retriever != null) {
                    retriever.release();
                }
            } catch (Exception ignore) {
            }
        }

        if (rotation == 90 || rotation == 270) {
            int temp = width;
            width = height;
            height = temp;
        }

        if (width <= 0 || height <= 0) {
            width = 360;
            height = 360;
        }

        int side = Math.min(width, height);
        int resultSide = 360;

        VideoEditedInfo info = new VideoEditedInfo();
        info.roundVideo = true;
        info.startTime = 0;
        info.endTime = duration > 0 ? Math.min(duration, 60000) : 60000;
        info.estimatedDuration = info.endTime - info.startTime;
        info.framerate = 25;
        info.bitrate = 1200000;
        info.originalPath = videoPath;
        info.originalWidth = width;
        info.originalHeight = height;
        info.resultWidth = resultSide;
        info.resultHeight = resultSide;
        info.rotationValue = rotation;
        info.muted = false;

        MediaController.CropState cropState = new MediaController.CropState();
        if (width > height) {
            cropState.cropPw = (float) side / width;
            cropState.cropPh = 1.0f;
            cropState.cropPx = (float) (width - side) / (2.0f * width);
            cropState.cropPy = 0.0f;
        } else if (height > width) {
            cropState.cropPw = 1.0f;
            cropState.cropPh = (float) side / height;
            cropState.cropPx = 0.0f;
            cropState.cropPy = (float) (height - side) / (2.0f * height);
        } else {
            cropState.cropPw = 1.0f;
            cropState.cropPh = 1.0f;
            cropState.cropPx = 0.0f;
            cropState.cropPy = 0.0f;
        }
        cropState.transformWidth = resultSide;
        cropState.transformHeight = resultSide;
        cropState.cropScale = 1.0f;
        cropState.initied = true;
        info.cropState = cropState;

        int encoderBitrate = MediaController.extractRealEncoderBitrate(info.resultWidth, info.resultHeight, info.bitrate);
        info.estimatedSize = (long) (info.estimatedDuration / 1000.0f * encoderBitrate / 8);
        if (info.estimatedSize <= 0) {
            File f = new File(videoPath);
            info.estimatedSize = f.exists() ? f.length() : 1;
        }

        return info;
    }

    public static void sendAsVideoNote(ChatActivity fragment, String videoPath, boolean notify, int scheduleDate) {
        if (fragment == null || videoPath == null) {
            return;
        }

        VideoEditedInfo info = createRoundVideoEditedInfo(videoPath);
        if (info == null) {
            return;
        }

        SendMessagesHelper.prepareSendingVideo(
                fragment.getAccountInstance(),
                videoPath,
                info,
                fragment.getDialogId(),
                fragment.getReplyMessage(),
                fragment.getThreadMessage(),
                null,
                null,
                0,
                null,
                notify,
                scheduleDate,
                false,
                false
        );

        BulletinFactory.of(fragment).createSimpleBulletin(
                R.raw.info,
                LocaleController.getString("SendingAsVideoNote", R.string.SendingAsVideoNote)
        ).show();
    }

    public static void convertMessageToVideoNote(ChatActivity fragment, MessageObject messageObject) {
        if (fragment == null || messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        File file = FileLoader.getInstance(fragment.getCurrentAccount()).getPathToMessage(messageObject.messageOwner);
        if (file != null && file.exists()) {
            sendAsVideoNote(fragment, file.getAbsolutePath(), true, 0);
            return;
        }

        if (messageObject.messageOwner.attachPath != null) {
            File attachFile = new File(messageObject.messageOwner.attachPath);
            if (attachFile.exists()) {
                sendAsVideoNote(fragment, attachFile.getAbsolutePath(), true, 0);
                return;
            }
        }

        BulletinFactory.of(fragment).createSimpleBulletin(
                R.raw.info,
                LocaleController.getString("DownloadingForVideoNote", R.string.DownloadingForVideoNote)
        ).show();

        FileLoader.getInstance(fragment.getCurrentAccount()).loadFile(
                messageObject.getDocument(),
                messageObject,
                FileLoader.PRIORITY_NORMAL,
                0
        );
    }
}
