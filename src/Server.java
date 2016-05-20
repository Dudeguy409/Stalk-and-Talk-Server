import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class Server {
	private XStream xmlStream = new XStream(new DomDriver());
	private static int SERVER_PORT_NUMBER = 49000;
	private static final int MAX_WAITING_CONNECTIONS = 300;
	private HttpServer server;
	private ActiveUserManager activeMgr;
	private UserProximityManager proxMgr;

	public static void main(String[] args) {
		Database.initialize();
		new Server().run();
	}

	/**
	 * 
	 * Creates a new HTTP Server with a new socket to await incoming
	 * connections. Then, creates separate HTTP handlers to handle each type of
	 * request. Finally, starts the server.
	 * 
	 * @throws DatabaseException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void run() {
		this.activeMgr = new ActiveUserManager();
		this.proxMgr = new UserProximityManager(this.activeMgr);

		// TODO comment or uncomment this line when you need to reset the DB
		// try {
		// this.resetDatabase();
		// } catch (Exception e1) {
		// e1.printStackTrace();
		// }

		new Thread(this.activeMgr).start();
		new Thread(proxMgr).start();

		try {
			// Creates a new HTTP Server with a new socket to await incoming
			// connections from a client.
			server = HttpServer.create(new InetSocketAddress(SERVER_PORT_NUMBER), MAX_WAITING_CONNECTIONS);
		} catch (IOException e) {
			System.out.println("Could not create HTTP server: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		server.setExecutor(null); // use the default executor

		// creates separate HTTP handlers to handle each type of request.
		server.createContext("/heartbeat", heartbeatHandler);
		server.createContext("/user/register", registrationHandler);
		server.createContext("/user/login", loginHandler);
		server.createContext("/user/delete", profileDeleteHandler);
		server.createContext("/user/update", profileUpdateHandler);
		server.createContext("/message/send", sendMessageHandler);
		server.createContext("/users/pic/put", updatePicHandler);
		server.createContext("/users/pic/delete", deletePicHandler);
		server.createContext("/users/pic", getPicHandler);
		server.createContext("/search", searchHandler);

		// TODO consider adding this in eventually
		// server.createContext("/users/username", getUserByNameHandler);

		server.start();
	}

	// authenticate
	// parse gps coords from header, parse distance, get results from
	// datastructure, remove self from it, get profiles, build a results obj,
	// write to xml
	private HttpHandler searchHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			ArrayList<User> finRslts;
			try {

				Headers headers = exchange.getRequestHeaders();
				String srcCoords = headers.getFirst("coords");
				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String distString = headers.getFirst("dist");

				if (srcCoords == null || username == null || password == null || distString == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int dist = Integer.parseInt(distString);
				int userID = Server.this.authenticate(username, password);

				ArrayList<Integer> intermediateRslts = Server.this.proxMgr.getNearbyUsers(srcCoords, dist);
				finRslts = new ArrayList<User>();

				db.startTransaction();
				for (Integer id : intermediateRslts) {
					if (id != userID) {
						User usr = db.getUserByID(id);
						finRslts.add(usr);
					}
				}
				db.endTransaction(true);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with the result attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			xmlStream.toXML(finRslts, exchange.getResponseBody());
			exchange.getResponseBody().close();
		}
	};

	// authenticate
	// parse the userID from the header and write the file to the response
	private HttpHandler getPicHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			File f;
			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String picUserIDString = headers.getFirst("pic-user-id");

				if (username == null || password == null || picUserIDString == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				// This line is still needed. Even though we don't need their
				// ID, we need to make sure they are a registered user. This
				// will throw an exception if they aren't.
				Server.this.authenticate(username, password);

				int picUserID = Integer.parseInt(picUserIDString);

				f = new File("pictures/" + picUserID);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with the picture they wanted attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			Files.copy(f.toPath(), exchange.getResponseBody());
			exchange.getResponseBody().close();
		}
	};

	// TODO This doesn't seem to work. The image file is not valid at the end.
	// Also, how do we handle different image types and extensions?

	// authenticate
	// read in image and parse user id from the query string and set that
	// a picture was uploaded and add the picture, overwriting if needed
	private HttpHandler updatePicHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			int userID;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				// String fileExtension = headers.getFirst("file-extension");

				if (username == null || password == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				userID = Server.this.authenticate(username, password);

				File f = new File("pictures/" + userID);

				db.startTransaction();
				db.updateHasPic(userID, true);
				db.endTransaction(true);

				Files.copy(exchange.getRequestBody(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
				exchange.getRequestBody().close();

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			exchange.close();
		}
	};

	// TODO handle deleting files of diff extensions? See updatepichandler

	// authenticate
	// parse user id from the query string and set that
	// a picture was deleted and delete the picture
	private HttpHandler deletePicHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			int userID;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");

				if (username == null || password == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				userID = Server.this.authenticate(username, password);

				File f = new File("pictures/" + userID);

				db.startTransaction();
				db.updateHasPic(userID, false);
				db.endTransaction(true);

				Files.deleteIfExists(f.toPath());

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			exchange.close();
		}
	};

	// authenticate
	// parse msg to send, to userID, write to DB, then return the msg ID
	private HttpHandler sendMessageHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			int msgID;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String toUserIDString = headers.getFirst("to-user-id");
				String contents = headers.getFirst("contents");

				if (username == null || password == null || toUserIDString == null || contents == null || contents.trim().isEmpty()) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int userID = Server.this.authenticate(username, password);
				int toUserID = Integer.parseInt(toUserIDString);				

				db.startTransaction();
				msgID = db.putMessage(userID, toUserID, contents);
				db.endTransaction(true);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			String idString = "" + msgID;
			exchange.getResponseBody().write(idString.getBytes());
			exchange.getResponseBody().close();
		}
	};

	// authenticate
	// pull msgs, write update in xml, parse gps coords, add to list
	private HttpHandler heartbeatHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			Update up;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String coords = headers.getFirst("coords");


				if (username == null || password == null || coords == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int userID = Server.this.authenticate(username, password);				

				db.startTransaction();
				up = db.pullMessages(userID);
				db.endTransaction(true);
				
				Server.this.activeMgr.addUser(userID, coords);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			xmlStream.toXML(up, exchange.getResponseBody());
			exchange.getResponseBody().close();
		}
	};

	// parse necessary headers, try to add user, then write the int
	// id into response
	private HttpHandler registrationHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();
			int userID;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String aboutMe = headers.getFirst("about-me");
				String emailAddress = headers.getFirst("email-address");
				String ageString = headers.getFirst("age");

				if (username == null || password == null || aboutMe == null || emailAddress == null
						|| ageString == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int age = Integer.parseInt(ageString);

				db.startTransaction();
				userID = db.addUser(username, password, aboutMe, age, emailAddress);
				db.endTransaction(true);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			String idString = "" + userID;
			exchange.getResponseBody().write(idString.getBytes());
			exchange.getResponseBody().close();
		}
	};

	// authenticate and write int id into response
	private HttpHandler loginHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			int userID;

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");

				if (username == null || password == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				userID = Server.this.authenticate(username, password);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			String idString = "" + userID;
			exchange.getResponseBody().write(idString.getBytes());
			exchange.getResponseBody().close();
		}
	};

	// authenticate
	// delete profile, then delete pic, then return success
	private HttpHandler profileDeleteHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");

				if (username == null || password == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int userID = Server.this.authenticate(username, password);				


				File f = new File("pictures/" + userID);
				Files.deleteIfExists(f.toPath());
				
				db.startTransaction();
				db.deleteUser(userID);
				db.endTransaction(true);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			exchange.close();
		}
	};

	// authenticate
	// update fields and return success
	private HttpHandler profileUpdateHandler = new HttpHandler() {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Database db = new Database();

			try {

				Headers headers = exchange.getRequestHeaders();

				String username = headers.getFirst("username");
				String password = headers.getFirst("password");
				String aboutMe = headers.getFirst("about-me");
				String ageString = headers.getFirst("age");

				if (username == null || password == null || aboutMe == null
						|| ageString == null) {
					throw new Exception("Not all of the proper header fields were included.");
				}

				int age = Integer.parseInt(ageString);


				int userID = Server.this.authenticate(username, password);				
				
				db.startTransaction();
				db.updateUser(userID, password, aboutMe, age);
				db.endTransaction(true);

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				try {
					// if any exception arise, the transaction is rolled back.
					db.endTransaction(false);
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
				// sends a 'fail' HTTP response in return if any issue comes up.
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
				return;
			}

			// since the request was successful, an HTTP response is sent back
			// to the client with their new userID attached.
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			exchange.close();
		}
	};

	public int authenticate(String username, String password) throws Exception {

		Database db = new Database();

		try {
			db.startTransaction();
			int userID = db.authenticate(username, password);
			db.endTransaction(true);
			return userID;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			try {
				// if any exception arise, the transaction is rolled back.
				db.endTransaction(false);
			} catch (Exception e1) {
				System.out.println(e1.getMessage());
				e1.printStackTrace();
			}
			throw new Exception("Error while authenticating");
		}
	}

	public void resetDatabase() throws Exception {

		Database db = new Database();

		try {
			db.startTransaction();
			db.resetDatabase();
			db.endTransaction(true);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			try {
				// if any exception arise, the transaction is rolled back.
				db.endTransaction(false);
			} catch (Exception e1) {
				System.out.println(e1.getMessage());
				e1.printStackTrace();
			}
			throw new Exception("Error while resetting database");
		}
	}
}
