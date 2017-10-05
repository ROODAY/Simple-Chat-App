Part 2

Uses Java skeleton code.
To compile, run javac Server.java and javac User.java.
To run, run java Server <port> <maxClients> and java User <host> <port> (can run multiple User clients).

Server takes portnumber and max client number as arguments (uses defaults of 8000 and 5).
Once running, waits for client connections.
When a new client joins, server informs all other connected clients with the #newuser protocol.
Server accepts messages from clients under the #status protocol.
Statuses are broadcasted to all other clients (the sender receives an acknowledgement).
Server can also accept a #Bye protocol message, to signal a user is leaving.
In that case, server will acknowledge sender and inform other clients.

Client takes hostname and portnumber as arguments (uses defaults of localhost and 8000).
Once running, waits for user input.
Upon receiving input, client prepends "#status " and sends message to server.
Client then receives message from server, which is then validated and printed.
Client can receive joining and leaving notifications for other clients, with protocols #newuser and #Leave respectively.
Client can receive broadcasted statuses from other clients, which are prepended with "newStatus".
Client can leave the server and quit the program by typing "Exit", which sends the #Bye protocol.

Current Extensions:
Server and Client have basic error handling in case of malformed protocols, both will simply log the malformed message so the user can debug.