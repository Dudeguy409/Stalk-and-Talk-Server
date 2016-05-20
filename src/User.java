
public class User {

	private int userID;
	private String username;
	private String aboutMe;
	private int age;
	private int points;
	private int exp;
	private boolean hasPic;

	public User(int userID, String username, String aboutMe, int age, int points, int exp, boolean hasPic) {
		this.userID = userID;
		this.username = username;
		this.aboutMe = aboutMe;
		this.age = age;
		this.points = points;
		this.exp = exp;
		this.hasPic = hasPic;
	}

}
