//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;

/*
 * A server that delivers status messages to other users.
 */
public class Server {
	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// Maximum number of groups 
	private static int maxGroupsCount = 5;
	// An array of threads for users
	private static userThread[] threads = null;

	public static void main(String args[]) {
		// The default port number.
		int portNumber = 8000;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		userThread[] threads = new userThread[maxUsersCount];

		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private ArrayList<userThread> friends; // Keeps track of user's friends.
	private ArrayList<String> groups; // Keeps track of all groups.
	private ArrayList<String> myGroups; // Keeps track of user's groups.
	private int maxUsersCount;
	private int maxGroupsCount; // For simplicity, set to 5.

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		this.friends = new ArrayList<userThread>();
		this.groups = new ArrayList<String>();
		this.myGroups = new ArrayList<String>();
		maxUsersCount = threads.length;
		maxGroupsCount = 5;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		int maxGroupsCount = this.maxGroupsCount;
		userThread[] threads = this.threads;
		ArrayList<userThread> friends = this.friends;
		ArrayList<String> groups = this.groups;
		ArrayList<String> myGroups = this.myGroups;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 * Welcome the new user. 
			 */
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			output_stream = new PrintStream(userSocket.getOutputStream());

			String joinMsg = input_stream.readLine();
			if (joinMsg.startsWith("#join")) { // Acknowledge new user joining, and inform other clients.
				userName = joinMsg.replace("#join", "").trim();
				System.out.println("User: " + userName + " has connected!");
				synchronized (userThread.class) {
					for (int i = 0; i < maxUsersCount; i++) {
						if (threads[i] == this) {
							output_stream.println("#welcome");
						} else if (threads[i] != null) {
							PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
							thread_os.printf("#newuser %s\n", userName);
						}
					}
				}
			} else { // Basic error handling.
				System.err.println("Unknown join message: " + joinMsg);
			}

			Boolean running = true; 

			/* Start the conversation. */
			while (running) {
				String clientMsg = input_stream.readLine();

				if (clientMsg.startsWith("#status")) { // Acknowledge sender of status and broadcast to other clients.
					String cleanedMsg = clientMsg.replace("#status", "").trim();
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] == this) {
								output_stream.println("#statusPosted");
							} else if (threads[i] != null && friends.indexOf(threads[i]) > -1) {
								PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
								thread_os.printf("#newStatus %s %s\n", userName, cleanedMsg);
							}
						}
					}
				} else if (clientMsg.startsWith("#friendme")) { // Takes incoming friend request and forwards it to requested user.
					Boolean requestSent = false;
					String friendName = clientMsg.replace("#friendme", "").trim();
					if (friendName.equals(userName)) { // Prevent self friending.
						output_stream.println("#error You can't friend yourself!");
					} else {
						synchronized (userThread.class) {
							for (int i = 0; i < maxUsersCount; i++) {
								if (threads[i] != null && threads[i].userName.equals(friendName)) {
									PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
									thread_os.printf("#friendme %s\n", userName);
									requestSent = true;
								}
							}
						}
						if (requestSent) { // If request was sent, acknowledge sender, or inform sender that requested user doesn't exist.
							output_stream.println("#serverMsg Friend Request Sent!");
						} else {
							output_stream.printf("#error User: %s doesn't exist.\n", friendName);
						}
					}
				} else if (clientMsg.startsWith("#friends")) { // Takes friend request acceptance and informs original sender.
					Boolean requestSent = false;
					String friendName = clientMsg.replace("#friends", "").trim();
					if (friendName.equals(userName)) { // Prevent self friending.
						output_stream.println("#error You can't friend yourself!");
					} else {
						synchronized (userThread.class) {
							for (int i = 0; i < maxUsersCount; i++) {
								if (threads[i] != null && threads[i].userName.equals(friendName)) {
									PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
									thread_os.printf("#OKfriends %s %s\n", friendName, userName);
									friends.add(threads[i]);       // Add requestor to this thread's friends
									threads[i].friends.add(this);  // Add this thread to requestor's friends
									requestSent = true;
								}
							}
						}
						if (requestSent) { // If original sender was acknowledged, inform the acceptor, or inform them that the user doesn't exist.
							output_stream.printf("#OKfriends %s %s\n", userName, friendName);
						} else {
							output_stream.printf("#error User: %s doesn't exist.\n", friendName);
						}
					}
				} else if (clientMsg.startsWith("#unfriend")) { // Takes unfriend request and forwards it to unfriended client
					Boolean requestSent = false;
					String friendName = clientMsg.replace("#unfriend", "").trim();
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] != null && threads[i].userName.equals(friendName)) {
								PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
								thread_os.printf("#NotFriends %s %s\n", friendName, userName);
								friends.remove(friends.indexOf(threads[i]));					// Remove the unfriended client from this thread's friends
								threads[i].friends.remove(threads[i].friends.indexOf(this));	// Remove this thread from unfriended client's friends
								requestSent = true;
							}
						}
					}
					if (requestSent) { // If unfriending was successful, tell client, or inform them the requested user to unfriend doesn't exist.
						output_stream.printf("#serverMsg You and %s are no longer friends!\n", friendName);
					} else {
						output_stream.printf("#error User: %s doesn't exist.\n", friendName);
					}
				} else if (clientMsg.startsWith("#DenyFriendRequest")) { // Takes deny friend request message and forwards it to original sender.
					Boolean msgSent = false;
					String requester = clientMsg.replace("#DenyFriendRequest", "").trim();
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] == this) {
								output_stream.println("#serverMsg Friend Request Denied!");
							} else if (threads[i] != null && threads[i].userName.equals(requester)) {
								PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
								thread_os.printf("#FriendRequestDenied %s\n", userName);
								msgSent = true;
							}
						}
					}
					if (!msgSent) { // If user doesn't exist, inform this client.
						output_stream.printf("#error Username: %s doesn't exist.\n", requester);
					}
				} else if (clientMsg.startsWith("#group")) { // Takes request to add user to a new group and processes it.
					String[] request = clientMsg.replace("#group", "").trim().split(" ");
					synchronized (userThread.class) {
						Boolean addUser = true;
						Boolean userAdded = false;

						if (groups.indexOf(request[0]) < 0) { // Check if group exists.
							if (groups.size() <= maxGroupsCount) { // Group doesn't exist, check if we have space for a new one.
								groups.add(request[0]); // Create new group and acknowledge sender.
								output_stream.printf("#serverMsg Group: %s created!\n", request[0]);
								for (int i = 0; i < maxUsersCount; i++) { // Inform clients of new group.
									if (threads[i] != null && threads[i] != this) {
										threads[i].groups.add(request[0]);
										PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
										thread_os.printf("#serverMsg User: %s created group: %s!\n", userName, request[0]);
									}
								}
							} else { // If max number of groups exist, inform sender.
								output_stream.println("#error The maximum number of groups has been reached!");
								addUser = false;
							}
						} else if (groups.indexOf(request[0]) > -1 && myGroups.indexOf(request[0]) < 0) { // If group already exists but sender is not a member, send them an error.
							output_stream.println("#error You must be a group member to add another member!");
							addUser = false;
						}

						if (addUser) { // Will be true if the above block ran succesfully (either sender is in group or created new group).
							if (myGroups.indexOf(request[0]) < 0) { // If sender is not already in the group, add them.
								myGroups.add(request[0]);
								output_stream.printf("#serverMsg Joined group: %s!\n", request[0]);
							}

							for (int i = 0; i < maxUsersCount; i++) { // Add the requested user to the group if friends, else send error to sender.
								if (threads[i] != null && threads[i].userName.equals(request[1])) {
									if (friends.indexOf(threads[i]) > -1) {
										if (threads[i].groups.indexOf(request[0]) < 0) {
											threads[i].groups.add(request[0]);
										}
										if (threads[i].myGroups.indexOf(request[0]) < 0) {
											threads[i].myGroups.add(request[0]);
										}
										PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
										thread_os.printf("#serverMsg User: %s added you to group: %s!\n", userName, request[0]); // Acknowledge requested user.
										userAdded = true;
									} else {
										output_stream.printf("#error You can't add user: %s to group: %s, you must be friends first!\n", request[1], request[0]);
									}
								}
							}
						}

						if (userAdded) { // If the above block ran succesfully (the request user was added), inform other clients.
							for (int i = 0; i < maxUsersCount; i++) {
								if (threads[i] != null && threads[i].groups.indexOf(request[0]) > -1 && !threads[i].userName.equals(request[1])) {
									PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
									thread_os.println(clientMsg);
								}
							}
						}
					}
				} else if (clientMsg.startsWith("#ungroup")) { // Takes request to remove a user from a group and processes it.
					String[] request = clientMsg.replace("#ungroup", "").trim().split(" ");
					Boolean deleteGroup = true;
					synchronized (userThread.class) {
						if (groups.indexOf(request[0]) > -1 && myGroups.indexOf(request[0]) > -1) { // Make sure the sender is a part of the group, else send them an error.
							for (int i = 0; i < maxUsersCount; i++) {
								if (threads[i] != null && threads[i].userName.equals(request[1])) { // If the requested user is in the specified group, remove them, else send the sender an error.
									if (threads[i].myGroups.indexOf(request[0]) > -1) {
										threads[i].myGroups.remove(threads[i].myGroups.indexOf(request[0]));
										PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
										thread_os.printf("#serverMsg User: %s removed you from group: %s!\n", userName, request[0]);
									} else {
										output_stream.printf("#error User: %s is not a part of group: %s!\n", request[1], request[0]);
									}
								} else if (threads[i] != null && threads[i].groups.indexOf(request[0]) > -1) { // Inform other clients in the group that the user was removed.
									PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
									thread_os.println(clientMsg);
								}
							}

							for (int i = 0; i < maxUsersCount; i++) { // If no one is in the group anymore, flag it for deletion.
								if (threads[i] != null && threads[i].myGroups.indexOf(request[0]) > -1) {
									deleteGroup = false;
								}
							}

							if (deleteGroup) { // If group was flagged for deletion, remove it from all thread's groups list and inform clients.
								for (int i = 0; i < maxUsersCount; i++) {
									if (threads[i] != null) {
										threads[i].groups.remove(threads[i].groups.indexOf(request[0]));
										PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
										thread_os.printf("#serverMsg Group: %s has no members, so it was deleted!\n", request[0]);
									}
								}
							}
						} else {
							output_stream.println("#error You are not part of that group or it doesn't exist!");
						}	
					}
				} else if (clientMsg.startsWith("#gstatus")) { // Takes group status message and broadcasts it to the specified group.
					String[] request = clientMsg.replace("#gstatus", "").trim().split(" ", 2);
					synchronized (userThread.class) {
						if (groups.indexOf(request[0]) > -1 && myGroups.indexOf(request[0]) > -1) { // Make sure sender is part of the group, else send them an error.
							for (int i = 0; i < maxUsersCount; i++) {
								if (threads[i] == this) {
									output_stream.println("#statusPosted"); // Acknowledge sender.
								} else if (threads[i] != null && threads[i].myGroups.indexOf(request[0]) > -1) {
									PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
									thread_os.printf("#newGStatus %s %s %s\n", request[0], userName, request[1]); // Broadcast status.
								}
							}
						} else {
							output_stream.println("#error You are not part of that group or it doesn't exist!");
						}	
					}
				} else if (clientMsg.startsWith("#Bye")) { // If message is a leave request, acknowledge the sender and inform other clients
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] == this) {
								output_stream.println("#Bye");
								System.out.println("User: " + userName + " has disconnected!");
							} else if (threads[i] != null) {
								PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
								thread_os.printf("#Leave %s\n", userName);
								if (threads[i].friends.indexOf(this) > -1) {
									threads[i].friends.remove(threads[i].friends.indexOf(this));
								}
							}
						}
					}
					running = false;
				} else { // Basic error handling for unknown protocols.
					System.err.println("Received unknown message type from client: " + clientMsg);
				}
			}

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
}