import java.util.ArrayList;

public class Update {
	private ArrayList<Message> messagesReceived;
	private ArrayList<Integer> messagesDelivered;
	private ArrayList<Integer> messagesFailed;

	public Update(ArrayList<Message> messagesReceived, ArrayList<Integer> messagesDelivered, ArrayList<Integer> messagesFailed) {
		this.messagesReceived = messagesReceived;
		this.messagesDelivered = messagesDelivered;
		this.messagesFailed = messagesFailed;
	}

}
