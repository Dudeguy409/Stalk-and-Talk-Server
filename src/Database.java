import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Database {
	private Connection connection = null;

	public static void initialize() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Establishes a database connection and then starts a database transaction.
	 * 
	 * @throws Exception
	 */
	public void startTransaction() throws Exception {
		// file address of the database within this project's main folder:
		String dbName = "database/stalkerDB.sqlite";
		String connectionURL = "jdbc:sqlite:" + dbName;

		this.connection = null;
		try {
			// Opens a database connection
			this.connection = DriverManager.getConnection(connectionURL);

			// from now on, no changes will be committed until all work is done,
			// and
			// the endTransaction method is called
			this.connection.setAutoCommit(false);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new Exception("Error with Database");
		}

	}

	/**
	 * ends and submits the current transaction and resets the connection.
	 * 
	 * @param commit
	 *            - whether the transaction succeeded or not.
	 * @throws SQLException
	 */
	public void endTransaction(boolean commit) throws Exception {

		try {
			if (commit) {
				connection.commit();
			} else {
				connection.rollback();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Error with Database");
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Error with Database");
			}
			connection = null;
		}

	}

	public void resetDatabase() {
		Statement resetStmt = null;

		// The SQL query statements to be executed in string form:
		String dropUsersString = "drop table if exists Users;";
		String dropMessagesString = "drop table if exists Messages;";
		String dropMessageStatusesString = "DROP TABLE IF EXISTS MessageStatuses;";

		String userString = "CREATE TABLE Users ( UserID integer not null primary key autoincrement, UserName varchar(255) not null UNIQUE, PassWord varchar(255), AboutMe varchar(1023), Age int, EMailAddress varchar(255) , Exp int, Points int, Pic int);";
		String messageStatusCreateString = "CREATE TABLE MessageStatuses (MessageStatus varchar(20));";
		String messageStatusInsertString = "INSERT INTO MessageStatuses (MessageStatus) Values ('Undelivered'), ('Error'), ('delivered');";
		String messageString = "CREATE TABLE Messages ( MessageID integer not null primary key autoincrement, Contents varchar(255), MessageStatus varchar(20), FromUserID Integer, ToUserID Integer,  FOREIGN KEY(FromUserID) REFERENCES Users(UserID), FOREIGN KEY(ToUserID) REFERENCES Users(UserID), FOREIGN KEY(MessageStatus) REFERENCES MessageStatuses(MessageStatus));";

		try {

			resetStmt = this.connection.createStatement();

			// drops and tables that may already exist.
			resetStmt.execute(dropUsersString);
			resetStmt.execute(dropMessagesString);
			resetStmt.execute(dropMessageStatusesString);

			// creates new, empty tables for the database.
			resetStmt.execute(userString);
			resetStmt.execute(messageStatusCreateString);
			resetStmt.execute(messageStatusInsertString);
			resetStmt.execute(messageString);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (resetStmt != null)
				try {
					resetStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	// TODO hash passwords
	public int authenticate(String username, String password) throws Exception {

		PreparedStatement authStmt = null;
		ResultSet rs = null;
		int userID = -1;

		String authString = "SELECT UserID, Password FROM Users WHERE UserName=?;";
		// TODO check that it works
		try {
			authStmt = this.connection.prepareStatement(authString);

			authStmt.setString(1, username);

			rs = authStmt.executeQuery();

			if (!rs.next()) {
				throw new Exception("User does not exist.");
			}
			if (!rs.getString("Password").equals(password)) {
				throw new Exception("Password doesn't match.");
			}

			userID = rs.getInt("UserID");

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (authStmt != null)
					authStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}

		return userID;
	}

	// This method should only be used when
	// pulling a friend whose username you know.
	public User getUserByName(String username) throws Exception {
		PreparedStatement getStmt = null;
		ResultSet rs = null;

		if (!isValidUsername(username)) {
			throw new Exception("This username does not have a valid format.");
		}

		String getString = "SELECT UserID, AboutMe, Age, Points, Exp, Pic FROM Users WHERE UserName=?;";

		try {
			getStmt = this.connection.prepareStatement(getString);
			getStmt.setString(1, username);

			rs = getStmt.executeQuery();
			if (!rs.next()) {
				throw new Exception("User does not exist.");
			}

			int userID = rs.getInt("UserID");
			String aboutMe = rs.getString("AboutMe");
			int age = rs.getInt("Age");
			int points = rs.getInt("Points");
			int exp = rs.getInt("Exp");
			boolean hasPic = rs.getInt("Pic") > 0;

			return new User(userID, username, aboutMe, age, points, exp, hasPic);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (getStmt != null)
					getStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	public User getUserByID(int userID) throws Exception {
		PreparedStatement getStmt = null;
		ResultSet rs = null;

		String getString = "SELECT Username, AboutMe, Age, Points, Exp, Pic FROM Users WHERE UserID=?;";

		try {
			getStmt = this.connection.prepareStatement(getString);
			getStmt.setInt(1, userID);

			rs = getStmt.executeQuery();
			if (!rs.next()) {
				throw new Exception("User does not exist.");
			}

			String username = rs.getString("Username");
			String aboutMe = rs.getString("AboutMe");
			int age = rs.getInt("Age");
			int points = rs.getInt("Points");
			int exp = rs.getInt("Exp");
			boolean hasPic = rs.getInt("Pic") > 0;

			return new User(userID, username, aboutMe, age, points, exp, hasPic);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (getStmt != null)
					getStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	public void addXPToUser(int userID, int xpToAdd) throws Exception {
		if (xpToAdd <= 0) {
			throw new Exception("The number of points to add must be positive.");
		}
		PreparedStatement xpStmt = null;
		String updateString = "Update Users SET Exp = Exp + ?, Points = Points + ? WHERE UserID = ?;";

		try {
			xpStmt = this.connection.prepareStatement(updateString);
			xpStmt.setInt(1, xpToAdd);
			xpStmt.setInt(2, xpToAdd);
			xpStmt.setInt(3, userID);

			// TODO check this
			if (xpStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (xpStmt != null)
					xpStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}

	}

	public void updatePoints(int userID, int pointsToAdd) throws Exception {
		String updateString = "Update Users SET Points = Points ";
		if (pointsToAdd > 0) {
			updateString += "+";
		} else {
			updateString += "-";
		}
		updateString += " ? WHERE UserID = ?;";

		PreparedStatement ptsStmt = null;

		try {
			ptsStmt = this.connection.prepareStatement(updateString);
			ptsStmt.setInt(1, pointsToAdd);
			ptsStmt.setInt(2, userID);

			// TODO check this
			if (ptsStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (ptsStmt != null)
					ptsStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	public int addUser(String username, String password, String aboutMe, int age, String emailAddress)
			throws Exception {

		if (!isValidEmailAddress(emailAddress)) {
			throw new Exception("This email address does not have a valid format.");
		}

		if (!isValidAge(age)) {
			throw new Exception("The age supplied is invalid.");
		}

		if (!isValidUsername(username)) {
			throw new Exception("This username does not have a valid format.");
		}

		String addString = "INSERT INTO Users (Username, PassWord, AboutMe, Age, EMailAddress, Exp, Points, Pic) VALUES (?,?,?,?,?,0,0,0);";
		PreparedStatement addStmt = null;

		try {
			addStmt = this.connection.prepareStatement(addString, Statement.RETURN_GENERATED_KEYS);
			addStmt.setString(1, username);
			addStmt.setString(2, password);
			addStmt.setString(3, aboutMe);
			addStmt.setInt(4, age);
			addStmt.setString(5, emailAddress);

			// TODO check this
			if (addStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

			// This code is lightly sampled from a stack overflow post:
			// http://stackoverflow.com/questions/1915166/how-to-get-the-insert-id-in-jdbc
			ResultSet generatedKeys = addStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				return generatedKeys.getInt(1);
			} else {
				throw new SQLException("Creating user failed, no ID obtained.");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (addStmt != null)
					addStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	// TODO send an email whenever a profile is updated
	public void updateUser(int userID, String password, String aboutMe, int age) throws Exception {
		String updateString = "UPDATE Users SET PassWord = ?, AboutMe = ?, Age = ? WHERE UserID = ?;";
		PreparedStatement updateStmt = null;

		if (!isValidAge(age)) {
			throw new Exception("The age supplied is invalid.");
		}

		try {
			updateStmt = this.connection.prepareStatement(updateString);
			updateStmt.setString(1, password);
			updateStmt.setString(2, aboutMe);
			updateStmt.setInt(3, age);
			updateStmt.setInt(4, userID);

			// TODO check this
			if (updateStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (updateStmt != null)
					updateStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}

	}

	// TODO make sure that you delete picture as well
	public void deleteUser(int userID) throws Exception {
		String deleteUserString = "DELETE FROM Users WHERE UserID = ?;";
		String deleteMessageString = "DELETE FROM Messages WHERE FromUserID = ?;";
		String updateMessageString = "UPDATE Messages SET MessageStatus = 'Failed' WHERE ToUserID = ?;";

		PreparedStatement delUsersStmt = null;
		PreparedStatement delMsgsStmt = null;
		PreparedStatement upMsgsStmt = null;

		try {
			delUsersStmt = this.connection.prepareStatement(deleteUserString);
			delUsersStmt.setInt(1, userID);
			delMsgsStmt = this.connection.prepareStatement(deleteMessageString);
			delMsgsStmt.setInt(1, userID);
			upMsgsStmt = this.connection.prepareStatement(updateMessageString);
			upMsgsStmt.setInt(1, userID);

			// TODO check this
			if (delUsersStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

			delMsgsStmt.executeUpdate();
			upMsgsStmt.executeUpdate();

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (delUsersStmt != null)
					delUsersStmt.close();
				if (delMsgsStmt != null)
					delMsgsStmt.close();
				if (upMsgsStmt != null)
					upMsgsStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	public void updateHasPic(int userID, boolean hasPic) throws Exception {
		String updateString = "UPDATE Users SET Pic = ? WHERE UserID = ?;";
		int pic = 0;
		if (hasPic) {
			pic = 1;
		}

		PreparedStatement updateStmt = null;

		try {
			updateStmt = this.connection.prepareStatement(updateString);
			updateStmt.setInt(1, pic);
			updateStmt.setInt(2, userID);

			// TODO check this
			if (updateStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (updateStmt != null)
					updateStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	// TODO throw error if user does not exist?
	public int putMessage(int fromUserID, int toUserID, String contents) throws Exception {
		String putString = "INSERT INTO Messages (FromUserID, ToUserID, Contents, MessageStatus) Values (?,?,?, 'Undelivered');";
		PreparedStatement putStmt = null;

		try {
			putStmt = this.connection.prepareStatement(putString, Statement.RETURN_GENERATED_KEYS);
			putStmt.setInt(1, fromUserID);
			putStmt.setInt(2, toUserID);
			putStmt.setString(3, contents);

			// TODO check this
			if (putStmt.executeUpdate() == 0) {
				throw new Exception("Database Error");
			}

			// This code is lightly sampled from a stack overflow post:
			// http://stackoverflow.com/questions/1915166/how-to-get-the-insert-id-in-jdbc
			ResultSet generatedKeys = putStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				return generatedKeys.getInt(1);
			} else {
				throw new SQLException("Putting message failed, no ID obtained.");
			}

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (putStmt != null)
					putStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
	}

	public Update pullMessages(int userID) throws Exception {
		String receivedString = "Select MessageID, FromUserId, ToUserID, Contents From Messages Where ToUserID = ? AND MessageStatus = 'Undelivered';";
		String deliveredString = "Select MessageID, MessageStatus From Messages Where FromUserID = ? AND (MessageStatus = 'Delivered' OR MessageStatus = 'Failed');";

		String updateReceivedString = "UPDATE Messages SET MessageStatus = 'Delivered' WHERE ";
		String deleteDeliveredString = "DELETE FROM Messages Where ";

		Update up = null;

		PreparedStatement recQueryStmt = null;
		PreparedStatement delivQueryStmt = null;

		PreparedStatement recUpdateStmt = null;
		PreparedStatement delivUpdateStmt = null;

		try {
			recQueryStmt = this.connection.prepareStatement(receivedString);
			recQueryStmt.setInt(1, userID);
			delivQueryStmt = this.connection.prepareStatement(deliveredString);
			delivQueryStmt.setInt(1, userID);

			ArrayList<Message> messagesReceived = new ArrayList<Message>();
			ArrayList<Integer> messagesDelivered = new ArrayList<Integer>();
			ArrayList<Integer> messagesFailed = new ArrayList<Integer>();

			ResultSet recRS = recQueryStmt.executeQuery();
			ResultSet delivRS = delivQueryStmt.executeQuery();

			boolean firstRec = true;
			while (recRS.next()) {
				int msgID = recRS.getInt("MessageID");
				int toID = recRS.getInt("ToUserID");
				int fromID = recRS.getInt("FromUserID");
				String contents = recRS.getString("Contents");

				messagesReceived.add(new Message(msgID, toID, fromID, contents));

				if (firstRec) {
					firstRec = false;
				} else {
					updateReceivedString += " OR ";
				}
				updateReceivedString += " MessageID = " + msgID;
			}

			updateReceivedString += ";";

			boolean firstDeliv = true;
			while (delivRS.next()) {
				int msgID = delivRS.getInt("MessageID");
				if ((delivRS.getString("MessageStatus")).equals("Delivered")) {
					messagesDelivered.add(msgID);
				} else {
					messagesFailed.add(msgID);
				}

				if (firstDeliv) {
					firstDeliv = false;
				} else {
					deleteDeliveredString += " OR ";
				}
				deleteDeliveredString += " MessageID = " + msgID;
			}

			deleteDeliveredString += ";";

			if (!firstDeliv) {
				delivUpdateStmt = this.connection.prepareStatement(deleteDeliveredString);

				// TODO check this
				if (delivUpdateStmt.executeUpdate() == 0) {
					throw new Exception("Database Error");
				}
			}

			if (!firstRec) {
				recUpdateStmt = this.connection.prepareStatement(updateReceivedString);

				// TODO check this
				if (recUpdateStmt.executeUpdate() == 0) {
					throw new Exception("Database Error");
				}
			}

			up = new Update(messagesReceived, messagesDelivered, messagesFailed);

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new Exception("Database Error");
		} finally {
			try {
				if (recQueryStmt != null)
					recQueryStmt.close();
				if (delivQueryStmt != null)
					delivQueryStmt.close();
				if (recUpdateStmt != null)
					recUpdateStmt.close();
				if (delivUpdateStmt != null)
					delivUpdateStmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
				throw new Exception("Database Error");
			}
		}
		return up;
	}

	// This REGEX was copied from a stack overflow post:
	// http://stackoverflow.com/questions/8204680/java-regex-email
	public boolean isValidEmailAddress(String emailAddress) {
		Pattern emailPat = Pattern.compile(
				"(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");
		return emailPat.matcher(emailAddress).matches();
	}

	public boolean isValidUsername(String username) {
		Pattern usrPat = Pattern.compile("^[a-z][a-z0-9_-]{4,15}$");
		return usrPat.matcher(username).matches();
	}

	public boolean isValidAge(int age) {
		return age > 5 && age < 115;
	}
}
