Part 1

Uses Java skeleton code.
To compile, run javac Server.java and javac User.java.
To run, run java Server and java User.

Server takes portnumber as optional argument (uses default of 8000).
Once running, waits for client connections (only 1 at a time).
Upon receiving a client message, server validates format, then prints message.
Server then sends acknowledgement to client that message was posted, then closes connection.
Server repeats this process until manually shut down.

Client takes hostname and portnumber as optional arguments (uses defaults of localhost and 8000).
Once running, waits for user input.
Upon receiving input, client prepends "#status " and sends message to server.
Client then receives message from server, which is then validated and printed.
Client then closes connection and exits.

Current Issue/Tradeoff: 
Server is designed for only one connection at a time, but will accept other clients.
All but the first client will be unable to post a message, and when the first client posts their message, all clients receive it.
This could be fixed by having a check on the server to ensure it is free before accepting a connection.