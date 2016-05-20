
public class Message {

	private int messageID;
	private int toUserID;
	private int fromUserID;
	private String contents;

	public Message(int msgID, int toID, int fromID, String contents) {
		this.messageID = msgID;
		this.toUserID = toID;
		this.fromUserID = fromID;
		this.contents = contents;
	}

}
