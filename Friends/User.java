//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;


public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;

	public static void main(String[] args) {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out
			.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {       
				// Get user name and join the social net
				System.out.print("Please enter a username: ");
				String userName = inputLine.readLine().trim();
				output_stream.println("#join " + userName);

				/* Create a thread to read from the server. */
				new Thread(new User()).start();

				

				while (!closed) {
					String userInput = inputLine.readLine().trim();
					if (userInput.equals("Exit")) {
						output_stream.println("#Bye");
					} else if (userInput.startsWith("@connect")) {
						String friend = userInput.replace("@connect", "").trim();
						output_stream.println("#friendme " + friend);
					} else if (userInput.startsWith("@friend")) {
						String friend = userInput.replace("@friend", "").trim();
						output_stream.println("#friends " + friend);
					} else if (userInput.startsWith("@deny")) {
						String friend = userInput.replace("@deny", "").trim();
						output_stream.println("#DenyFriendRequest " + friend);
					} else if (userInput.startsWith("@disconnect")) {
						String friend = userInput.replace("@disconnect", "").trim();
						output_stream.println("#unfriend " + friend);
					} else if (!userInput.startsWith("@")) {
						output_stream.println("#status " + userInput);
					} else {
						System.err.println("Unrecognized command!");
					}
				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */

				output_stream.close();
				input_stream.close();
				userSocket.close();
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine;
		
		try {
			while ((responseLine = input_stream.readLine()) != null) {
				if (responseLine.startsWith("#welcome")) {
					System.out.println("Connection established!");
					System.out.println("Type 'Exit' to quit.");
				} else if (responseLine.startsWith("#busy")) {
					System.err.println("Server is busy right now, please try again later!");
					System.exit(0);
					break;
				} else if (responseLine.startsWith("#statusPosted")) {
					System.out.println("Status posted!");
				} else if (responseLine.startsWith("#newuser")) {
					String joiningUser = responseLine.replace("#newuser", "").trim();
					System.out.println("User " + joiningUser + " has entered!");
				} else if (responseLine.startsWith("#newStatus")) {
					String newStatus[] = responseLine.replace("#newStatus", "").trim().split(" ", 2);
					System.out.printf("[%s] %s\n", newStatus[0], newStatus[1]);
				} else if (responseLine.startsWith("#Leave")) {
					String leavingUser = responseLine.replace("#Leave", "").trim();
					System.out.println("User " + leavingUser + " has left!");
				} else if (responseLine.startsWith("#friendme")) {
					String friendName = responseLine.replace("#friendme", "").trim();
					System.out.println("User " + friendName + " wants to be friends!");
					System.out.printf("Type '@friend %s' to accept!\n", friendName);
					System.out.printf("Type '@deny %s' to deny!\n", friendName);
				} else if (responseLine.startsWith("#OKfriends")) {
					String friendName = responseLine.replace("#OKfriends", "").trim().split(" ")[1];
					System.out.println("You and User " + friendName + " are now friends!");
				} else if (responseLine.startsWith("#FriendRequestDenied")) {
					String friendName = responseLine.replace("#FriendRequestDenied", "").trim();
					System.out.println("User " + friendName + " denied your friend request!");
				} else if (responseLine.startsWith("#NotFriends")) {
					String friendName = responseLine.replace("#NotFriends", "").trim().split(" ")[1];
					System.out.println("You are no longer friends with " + friendName + "!");
				} else if (responseLine.startsWith("#Bye")) {
					System.err.println("Closing connection...");
					System.exit(0);
					break;
				} else if (responseLine.startsWith("#error")) {
					String errorMsg = responseLine.replace("#error", "").trim();
					System.err.println("ERROR: " + errorMsg);
				} else if (responseLine.startsWith("#serverMsg")) {
					String serverMsg = responseLine.replace("#serverMsg", "").trim();
					System.out.println(serverMsg);
				} else {
					System.err.println("Received unknown message from server: " + responseLine);
				}
			}
			closed = true;
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}



