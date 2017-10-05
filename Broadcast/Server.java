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
	private int maxUsersCount;

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 * Welcome the new user. 
			 */
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			output_stream = new PrintStream(userSocket.getOutputStream());

			String joinMsg = input_stream.readLine();
			if (joinMsg.startsWith("#join")) { // When a new user joins the server, log to server, welcome the user, and broadcast to other connected users
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
			} else { // Basic error handling
				System.err.println("Unknown join message: " + joinMsg);
			}

			Boolean running = true; 

			/* Start the conversation. */
			while (running) {
				String clientMsg = input_stream.readLine();

				if (clientMsg.startsWith("#status")) { // If message is a status, acknowledge the sender and broadcast to other clients
					String cleanedMsg = clientMsg.replace("#status", "").trim();
					synchronized (userThread.class) {
						for (int i = 0; i < maxUsersCount; i++) {
							if (threads[i] == this) {
								output_stream.println("#statusPosted");
							} else if (threads[i] != null) {
								PrintStream thread_os = new PrintStream(threads[i].userSocket.getOutputStream());
								thread_os.printf("#newStatus %s %s\n", userName, cleanedMsg);
							}
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
							}
						}
					}
					running = false;
				} else { // Basic error handling
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