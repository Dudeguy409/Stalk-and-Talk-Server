
import java.util.HashMap;
import java.util.LinkedList;

public class ActiveUserManager implements Runnable {
	private LinkedList<HashMap<Integer, String>> mapQueue;

	public ActiveUserManager() {
		this.mapQueue = new LinkedList<HashMap<Integer, String>>();
		for (int i = 0; i < 6; i++) {
			this.mapQueue.add(new HashMap<Integer, String>());
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.mapQueue.offerFirst(new HashMap<Integer, String>());
			this.mapQueue.removeLast();
			//System.out.println("Five seconds has passed, and a map has been removed");
		}
	}

	public void addUser(int userID, String coords) {
		this.mapQueue.getFirst().put(userID, coords);
	}

	public LinkedList<HashMap<Integer, String>> getMapQueue() {
		return this.mapQueue;
	}

}
