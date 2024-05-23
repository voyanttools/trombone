package org.metodosdigitais;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
	
	@JsonProperty("_id")
	private String id;
	@JsonProperty("_index")
	private String index;
	@JsonProperty("_score")
	private int score;
	@JsonProperty("_source")
	private String source;
	@JsonProperty("all_text")
	private String allText;
	@JsonProperty("chat_id")
	private String chatId;
	@JsonProperty("chat_title")
	private String chatTitle;
	@JsonProperty("forward_chat_id")
	private String forwardChatId;
	@JsonProperty("forward_user_id")
	private String forwardUserId;
	@JsonProperty("from_user_id")
	private String fromUserId;
	@JsonProperty("from_user_is_bot")
	private boolean fromUserIsBot;
	@JsonProperty("from_user_is_deleted")
	private boolean fromUserIsDeleted;
	@JsonProperty("from_user_is_fake")
	private boolean fromUserIsFake;
	@JsonProperty("from_user_is_verified")
	private boolean fromUserIsVerified;
	@JsonProperty("message_caption")
	private String messageCaption;
	@JsonProperty("message_id")
	private int messageId;
	@JsonProperty("sender_chat_id")
	private String senderChatId;
	@JsonProperty("strict_date")
	private Date strictDate;
	@JsonProperty("strict_edit_date")
	private Date strictEditDate;
	@JsonProperty("text")
	private String text;
	@JsonProperty("type")
	private String type;
	@JsonProperty("views")
	private int views;
	@JsonProperty("word_count")
	private int wordCount;
	
	public ChatMessage() {}
	

	public ChatMessage(String id, String index, int score, String source, String allText, String chatId,
			String chatTitle, String forwardChatId, String forwardUserId, String fromUserId, boolean fromUserIsBot,
			boolean fromUserIsDeleted, boolean fromUserIsFake, boolean fromUserIsVerified, String messageCaption,
			int messageId, String senderChatId, Date strictDate, Date strictEditDate, String text, String type,
			int views, int wordCount) {
		super();
		this.id = id;
		this.index = index;
		this.score = score;
		this.source = source;
		this.allText = allText;
		this.chatId = chatId;
		this.chatTitle = chatTitle;
		this.forwardChatId = forwardChatId;
		this.forwardUserId = forwardUserId;
		this.fromUserId = fromUserId;
		this.fromUserIsBot = fromUserIsBot;
		this.fromUserIsDeleted = fromUserIsDeleted;
		this.fromUserIsFake = fromUserIsFake;
		this.fromUserIsVerified = fromUserIsVerified;
		this.messageCaption = messageCaption;
		this.messageId = messageId;
		this.senderChatId = senderChatId;
		this.strictDate = strictDate;
		this.strictEditDate = strictEditDate;
		this.text = text;
		this.type = type;
		this.views = views;
		this.wordCount = wordCount;
	}


	public String getId() {
		return id;
	}


	public String getIndex() {
		return index;
	}


	public int getScore() {
		return score;
	}


	public String getSource() {
		return source;
	}


	public String getAllText() {
		return allText;
	}


	public String getChatId() {
		return chatId;
	}


	public String getChatTitle() {
		return chatTitle;
	}


	public String getForwardChatId() {
		return forwardChatId;
	}


	public String getForwardUserId() {
		return forwardUserId;
	}


	public String getFromUserId() {
		return fromUserId;
	}


	public boolean isFromUserIsBot() {
		return fromUserIsBot;
	}


	public boolean isFromUserIsDeleted() {
		return fromUserIsDeleted;
	}


	public boolean isFromUserIsFake() {
		return fromUserIsFake;
	}


	public boolean isFromUserIsVerified() {
		return fromUserIsVerified;
	}


	public String getMessageCaption() {
		return messageCaption;
	}


	public int getMessageId() {
		return messageId;
	}


	public String getSenderChatId() {
		return senderChatId;
	}


	public Date getStrictDate() {
		return strictDate;
	}


	public Date getStrictEditDate() {
		return strictEditDate;
	}


	public String getText() {
		return text;
	}


	public String getType() {
		return type;
	}


	public int getViews() {
		return views;
	}


	public int getWordCount() {
		return wordCount;
	}


	@Override
	public String toString() {
		return "chat_title: "+this.chatTitle+", all_text: "+this.allText;
	}

	
}