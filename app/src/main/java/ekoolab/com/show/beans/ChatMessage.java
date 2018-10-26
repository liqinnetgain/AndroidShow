package ekoolab.com.show.beans;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.orhanobut.logger.Logger;
import com.sendbird.android.FileMessage;
import com.sendbird.android.SendBird;
import com.sendbird.android.UserMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ekoolab.com.show.R;
import ekoolab.com.show.beans.enums.FileType;
import ekoolab.com.show.beans.enums.MessageType;
import ekoolab.com.show.beans.enums.SendState;
import ekoolab.com.show.utils.AuthUtils;
import ekoolab.com.show.utils.Constants;
import ekoolab.com.show.utils.DataBaseManager;
import ekoolab.com.show.utils.Utils;

import static ekoolab.com.show.beans.enums.MessageType.AUDIO;
import static ekoolab.com.show.beans.enums.MessageType.PHOTO;
import static ekoolab.com.show.beans.enums.MessageType.VIDEO;

public class ChatMessage {

    public String message;
    public String channelUrl;
    public String senderId;
    public String senderName;
    public String senderProfileUrl;
    public long createAt;
    public long updateAt;
    public long messageId;
    public String requestId;
    public MessageType messageType;
    public SendState sendState;
    public ResourceFile resourceFile;
    public boolean isTemp;

    public ChatMessage() {
        isTemp = false;
        sendState = SendState.SENDING;
        messageType = MessageType.TEXT;
        createAt = System.currentTimeMillis();
    }

    public static ChatMessage createByComingUserMessage(Context context, UserMessage userMessage){
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.message = userMessage.getMessage();
        chatMessage.messageId = userMessage.getMessageId();
        chatMessage.requestId = userMessage.getRequestId();
        chatMessage.messageType = MessageType.getMessageType(userMessage.getCustomType());
        chatMessage.sendState = SendState.SENT;
        chatMessage.createAt = userMessage.getCreatedAt();
        chatMessage.updateAt = userMessage.getUpdatedAt();
        chatMessage.senderId = userMessage.getSender().getUserId();
        chatMessage.senderName = userMessage.getSender().getNickname();
        chatMessage.senderProfileUrl = userMessage.getSender().getProfileUrl();
        chatMessage.channelUrl = userMessage.getChannelUrl();
        return chatMessage;
    }

    public static ChatMessage createByOutgoingUserMessage(Context context, UserMessage userMessage){
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.message = userMessage.getMessage();
        chatMessage.channelUrl = userMessage.getChannelUrl();
        chatMessage.senderId = AuthUtils.getInstance(context).getUserCode();
        chatMessage.createAt = userMessage.getCreatedAt();
        chatMessage.updateAt = userMessage.getUpdatedAt();
        chatMessage.messageId = userMessage.getMessageId();
        chatMessage.requestId = userMessage.getRequestId();
        chatMessage.senderName = userMessage.getSender().getNickname();
        chatMessage.senderProfileUrl = userMessage.getSender().getProfileUrl();
        chatMessage.messageType = MessageType.getMessageType(userMessage.getCustomType());
        return chatMessage;
    }

    public static ChatMessage createByOutgoingFileMessage(Context context, FileMessage fileMessage, ResourceFile resourceFile){
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.channelUrl = fileMessage.getChannelUrl();
        chatMessage.senderId = AuthUtils.getInstance(context).getUserCode();
        chatMessage.createAt = fileMessage.getCreatedAt();
        chatMessage.updateAt = fileMessage.getUpdatedAt();
        chatMessage.messageId = fileMessage.getMessageId();
        chatMessage.requestId = fileMessage.getRequestId();
        chatMessage.senderName = fileMessage.getSender().getNickname();
        chatMessage.senderProfileUrl = fileMessage.getSender().getProfileUrl();
        chatMessage.messageType = MessageType.getMessageType(fileMessage.getCustomType());
        if (resourceFile.fileType == FileType.AUDIO) {
            chatMessage.message = context.getString(R.string.voice);
        }
        chatMessage.resourceFile = resourceFile;
        return chatMessage;
    }

    public static ChatMessage createTempChatMessage(Context context, File fileResource, FileType fileType){
        ChatMessage audioChatMessage = new ChatMessage();
        audioChatMessage.isTemp = true;
        audioChatMessage.message = context.getString(R.string.voice);
        if (fileType == FileType.AUDIO){
            audioChatMessage.messageType = MessageType.AUDIO;
        } else if (fileType == FileType.IMAGE) {
            audioChatMessage.messageType = MessageType.PHOTO;
        } else if(fileType == FileType.VIDEO){
            audioChatMessage.messageType = MessageType.VIDEO;
        } else if(fileType == FileType.DOCUMENT) {
            audioChatMessage.messageType = MessageType.OTHER;
        }

        if (fileResource != null) {
            audioChatMessage.resourceFile = new ResourceFile(context, fileResource, fileType);
        }

        return audioChatMessage;
    }

    public boolean save(Context context){
        synchronized(context){
            SQLiteDatabase db = DataBaseManager.getInstance(context).openDatabase();

            ContentValues values = new ContentValues();
            values.put(Constants.ChatMessageTableColumns.message, this.message);
            values.put(Constants.ChatMessageTableColumns.channelUrl, this.channelUrl);
            values.put(Constants.ChatMessageTableColumns.senderId, this.senderId);
            values.put(Constants.ChatMessageTableColumns.senderName, this.senderName);
            values.put(Constants.ChatMessageTableColumns.senderProfileUrl, this.senderProfileUrl);
            values.put(Constants.ChatMessageTableColumns.createAt, this.createAt);
            values.put(Constants.ChatMessageTableColumns.updateAt, this.updateAt);
            values.put(Constants.ChatMessageTableColumns.messageId, this.messageId);
            values.put(Constants.ChatMessageTableColumns.requestId, this.requestId);
            values.put(Constants.ChatMessageTableColumns.messageType, this.messageType.getIndex());
            values.put(Constants.ChatMessageTableColumns.sendState, this.sendState.getIndex());

            try{
                long id = db.insertWithOnConflict(Constants.CHAT_MESSAGE_TB, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id == -1) {
                    if (this.messageId > 0) {
                        db.update(Constants.FRIEND_TB, values,
                                Constants.ChatMessageTableColumns.messageId + "=?", new String[]{this.messageId+""});
                    }else{
                        db.update(Constants.FRIEND_TB, values,
                                Constants.ChatMessageTableColumns.requestId + "=?", new String[]{this.requestId});
                    }
                } else {
                    if(this.resourceFile != null) {
                        this.resourceFile.save(context, id);
                    }
                }
            }catch(SQLiteException e){
                e.printStackTrace();
            }finally {
                if (db != null){
                    DataBaseManager.getInstance(context).closeDatabase();
                }
            }

            return true;
        }
    }

    public static List<ChatMessage> getChatMessages(Context context, String selection, String[] args, String limit){
        SQLiteDatabase db = DataBaseManager.getInstance(context).openDatabase();
        List<ChatMessage> chatMessages = new ArrayList<>();
        Cursor cursor = null;
        try{
            cursor = db.query(Constants.CHAT_MESSAGE_TB, null, selection, args,
                    null, null, Constants.ChatMessageTableColumns.createAt + " ASC", limit);
            cursor.moveToFirst();
            ChatMessage chatMessage = null;
            while(!cursor.isAfterLast()){
                chatMessage = new ChatMessage();
                chatMessage.message = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.message));
                chatMessage.channelUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.channelUrl));
                chatMessage.senderId = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.senderId));
                chatMessage.senderName = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.senderName));
                chatMessage.senderProfileUrl = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.senderProfileUrl));
                chatMessage.createAt = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.createAt));
                chatMessage.updateAt = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.updateAt));
                chatMessage.messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.messageId));
                chatMessage.requestId = cursor.getString(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.requestId));
                chatMessage.messageType = MessageType.getMessageType(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.messageType)));
                chatMessage.sendState = SendState.getSendState(cursor.getInt(cursor.getColumnIndexOrThrow(Constants.ChatMessageTableColumns.sendState)));

                if(chatMessage.messageType == AUDIO || chatMessage.messageType == PHOTO || chatMessage.messageType == VIDEO){
                    long chatMessageId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                    if (chatMessageId > 0) {
                        chatMessage.resourceFile = ResourceFile.getResourceChatMessageId(context, chatMessageId);
                    }
                }

                chatMessages.add(chatMessage);
                cursor.moveToNext();
            }
        }catch(SQLiteException e){
            Logger.e(e.getLocalizedMessage());
        }finally{
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
            if (db != null){
                DataBaseManager.getInstance(context).closeDatabase();
            }
        }
        return chatMessages;
    }


}
