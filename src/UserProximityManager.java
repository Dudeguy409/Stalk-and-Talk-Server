import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

public class UserProximityManager implements Runnable {

	private LinkedList<HashMap<Integer, String>> mapQueue;

	public UserProximityManager(ActiveUserManager am) {
		this.mapQueue = am.getMapQueue();
	}

	// TODO this does nothing right now. In the future, when a 2d tree is
	// implemented, this will wake up every 15 or so seconds and redraw a
	// balanced 2d tree.
	@Override
	public void run() {
		// while (true) {
		// try {
		// Thread.sleep(15000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// //TODO redraw tree here
		// }
	}

	// This is the current crappy implementation. It is a linear search. This
	// will hopefully soon be replaced by a tree search.
	public ArrayList<Integer> getNearbyUsers(String srcCoords, int dist) {
		int rsltsFound = 0;
		ArrayList<Integer> rslts = new ArrayList<Integer>();

		Iterator<HashMap<Integer, String>> iter = this.mapQueue.iterator();

		while (iter.hasNext() && rsltsFound < 25) {
			HashMap<Integer, String> map = iter.next();

			for (Entry<Integer, String> e : map.entrySet()) {
				if (this.computeDistance(e.getValue(), srcCoords) <= dist) {
					rslts.add(e.getKey());
					rsltsFound++;
				}
				if (rsltsFound == 25) {
					break;
				}
			}
		}
		return rslts;
	}

	// TODO implement this. Also, consider passing
	// the values in as parsed GPS coordinates.
	// Consider using the haversine formula
	private int computeDistance(String destCoords, String srcCoords) {
		return 0;
	}

}
