# Java HTTP Server

A lightweight HTTP/1.1 server built from scratch in Java using raw TCP sockets.

The project implements core web-server functionality without relying on frameworks. It was built to explore how HTTP servers work internally, including request parsing, response generation, routing, static file serving, concurrency, middleware, cookies, and sessions.

## Features

- HTTP/1.1 request parsing
- GET and POST route registration
- Query parameter parsing
- HTTP header parsing
- Request body parsing using `Content-Length`
- URL decoding
- Structured HTTP responses
- HTTP status codes
- Dynamic route parameters
- Static file serving
- MIME type detection
- Directory traversal protection
- Fixed-size worker thread pool
- Graceful server shutdown
- Middleware pipeline
- Request timing
- Error handling
- Cookie parsing and generation
- In-memory sessions
- 400, 401, 403, 404, 405 and 500 responses
- Unit and integration testing with JUnit 5

## Why I Built This

Most Java web applications hide the HTTP layer behind frameworks.

This project takes the opposite approach.

Instead of starting with a framework, the server starts with:

```java
ServerSocket serverSocket = new ServerSocket(port);
```

and builds the HTTP abstractions on top of raw TCP connections.

The goal was to understand the path from:

```text
TCP connection
      ↓
raw HTTP bytes
      ↓
request parsing
      ↓
middleware
      ↓
routing
      ↓
handler
      ↓
HTTP response
      ↓
TCP connection
```

## Architecture

The server is split into focused components.

```text
Client
  │
  │ TCP
  ▼
HttpServer
  │
  ├── RequestParser
  │      ├── Request line
  │      ├── Headers
  │      ├── Query parameters
  │      ├── Cookies
  │      └── Body
  │
  ▼
Middleware Chain
  │
  ├── Error handling
  ├── Session handling
  ├── Logging
  └── Request timing
  │
  ▼
Router
  │
  ├── Exact routes
  └── Dynamic routes
  │
  ▼
RouteHandler
  │
  ▼
HttpResponse
  │
  ▼
ResponseWriter
  │
  ▼
Client
```

Static GET requests that do not match registered routes can be resolved through the static file handler.

## Example

Routes can be registered directly on the server:

```java
server.get(
        "/",
        (request, response) ->
                response.text("Hello World")
);

server.get(
        "/users/:id",
        (request, response) -> {
            String id =
                    request.getPathParameter("id");

            response.text("User " + id);
        }
);

server.post(
        "/users",
        (request, response) ->
                response
                        .status(HttpStatus.CREATED)
                        .json(
                                "{\"message\":\"User created\"}"
                        )
);
```

A request such as:

```http
GET /users/42 HTTP/1.1
Host: localhost:8080
```

is matched against:

```text
/users/:id
```

and exposes:

```java
request.getPathParameter("id");
```

as:

```text
42
```

## Request Parsing

`RequestParser` converts the incoming HTTP request into an `HttpRequest`.

It extracts:

- HTTP method
- request path
- HTTP version
- headers
- query parameters
- path parameters
- cookies
- request body

For example:

```http
GET /search?q=hello%20world&page=2 HTTP/1.1
Host: localhost:8080
```

becomes conceptually:

```text
Method: GET
Path: /search
Query:
    q = hello world
    page = 2
Version: HTTP/1.1
```

Malformed requests result in a `400 Bad Request` response.

## Response System

Responses are represented using `HttpResponse` rather than manually constructing HTTP strings inside the server.

Example:

```java
response
        .status(HttpStatus.CREATED)
        .header("X-Example", "value")
        .json("{\"status\":\"created\"}");
```

`ResponseWriter` converts the response into valid HTTP bytes.

Example output:

```http
HTTP/1.1 201 Created
Content-Type: application/json
Content-Length: 20
Connection: close

{"status":"created"}
```

`Content-Length` is calculated from the UTF-8 encoded body rather than the Java character count.

## Routing

Routes are matched using both HTTP method and path.

```text
GET  /users
POST /users
```

are treated as separate routes.

The router supports dynamic path parameters:

```text
/users/:id
/books/:bookId/chapters/:chapterId
```

For:

```text
GET /books/10/chapters/3
```

the handler can access:

```java
request.getPathParameter("bookId");
request.getPathParameter("chapterId");
```

Exact routes take precedence over dynamic routes.

Therefore, if both exist:

```text
/users/new
/users/:id
```

a request for:

```text
/users/new
```

matches the exact route.

## Static File Serving

The server can serve files from a configured public directory (in this case the dinosuar game).

For example:

```text
public/
├── index.html
├── styles.css
├── app.js
└── images/
```

A request for:

```text
GET /styles.css
```

can resolve to:

```text
public/styles.css
```

Supported MIME types include:

| Extension | Content Type |
|---|---|
| `.html` | `text/html` |
| `.css` | `text/css` |
| `.js` | `application/javascript` |
| `.json` | `application/json` |
| `.png` | `image/png` |
| `.jpg` | `image/jpeg` |
| `.svg` | `image/svg+xml` |
| `.txt` | `text/plain` |

Unknown types fall back to:

```text
application/octet-stream
```

### Path Security

Static paths are normalized before files are accessed.

Requests attempting to escape the configured public directory, such as:

```text
/../../passwords.txt
```

are rejected.

## Concurrency

The server uses a fixed-size `ExecutorService` worker pool.

```java
Executors.newFixedThreadPool(threadPoolSize);
```

The main server thread accepts connections and submits them to worker threads:

```text
Server thread
     │
     ├── accept connection
     │
     ▼
Worker pool
     │
     ├── parse request
     ├── execute middleware
     ├── route request
     ├── generate response
     └── close connection
```

A fixed-size pool prevents the server from creating an unlimited number of threads under load.

The server state uses appropriate concurrency controls, including a `volatile` running flag and thread-safe session storage.

## Graceful Shutdown

Calling:

```java
server.stop();
```

stops the server from accepting new connections and begins shutting down the worker pool.

The server:

1. Stops accepting new connections
2. Closes the server socket
3. Calls `ExecutorService.shutdown()`
4. Allows active requests time to complete
5. Uses `shutdownNow()` if workers do not terminate within the timeout

## Middleware

Middleware allows behavior to run around route handlers.

```java
server.use(new ErrorHandlingMiddleware());
server.use(new SessionMiddleware(sessionManager));
server.use(new LoggingMiddleware());
server.use(new TimingMiddleware());
```

Conceptually:

```text
Middleware A - before
    Middleware B - before
        Route Handler
    Middleware B - after
Middleware A - after
```

Middleware can:

- inspect requests
- modify responses
- measure execution time
- catch exceptions
- manage sessions
- stop request processing by not calling the next middleware

Timing middleware adds a response header such as:

```text
X-Response-Time: 3ms
```

## Cookies and Sessions

Incoming cookies are parsed from headers such as:

```http
Cookie: sessionId=abc123; theme=dark
```

and can be accessed with:

```java
request.getCookie("sessionId");
```

Responses can set cookies:

```java
response.cookie(
        "sessionId",
        sessionId
);
```

producing a header similar to:

```http
Set-Cookie: sessionId=abc123; Path=/; HttpOnly
```

### Sessions

Sessions are stored in memory using a `ConcurrentHashMap`.

```text
Browser
   │
   │ sessionId cookie
   ▼
SessionManager
   │
   ▼
Session
   │
   └── username = Alice
```

The browser stores only the session identifier. Session data remains on the server.

Example:

```java
Session session =
        request.getSession();

session.set(
        "username",
        "Alice"
);
```

The project includes simple demonstration login, profile and logout flows.

These routes demonstrate session persistence only and are **not intended to represent a production authentication system**.

## Error Handling

The server generates appropriate HTTP responses for common failure cases:

| Status | Meaning |
|---|---|
| `400 Bad Request` | Malformed HTTP request |
| `401 Unauthorized` | Authentication/session demonstration |
| `403 Forbidden` | Forbidden resource |
| `404 Not Found` | Route or static resource does not exist |
| `405 Method Not Allowed` | Path exists but HTTP method does not |
| `500 Internal Server Error` | Unexpected handler/server error |

Unexpected handler exceptions are caught so that one failed request does not terminate the server.

## Testing

The project uses JUnit 5.

The test suite covers components including:

### Request parsing

- GET and POST requests
- headers
- query parameters
- URL decoding
- request bodies
- malformed requests
- unsupported methods
- cookies

### Routing

- exact routes
- method-specific routes
- dynamic parameters
- multiple parameters
- route precedence
- missing routes
- duplicate registration

### Responses

- status lines
- headers
- text responses
- JSON responses
- UTF-8 content length
- empty bodies

### Server behaviour

- lifecycle
- thread-pool handling
- graceful shutdown
- middleware
- static files
- sessions

### Integration Tests

Integration tests start a real server on an available test port and communicate with it through TCP sockets.

They verify complete request/response flows such as:

```text
TCP socket
→ HttpServer
→ RequestParser
→ Middleware
→ Router
→ RouteHandler
→ HttpResponse
→ ResponseWriter
→ TCP socket
```

This includes testing successful requests as well as `400`, `404`, `405`, and `500` responses.

Run all tests with:

```bash
mvn clean test
```

## Running the Server

### Requirements

- Java 21
- Maven

Clone the repository and run:

```bash
mvn clean package
```

Then run the application through your IDE or Maven configuration.

By default, the example server runs at:

```text
http://localhost:8080
```

You can test it with a browser or:

```bash
curl http://localhost:8080/
```

On Windows PowerShell, use:

```powershell
curl.exe -i http://localhost:8080/
```

## Project Structure

```text
src/
├── main/
│   └── java/
│       └── org/example/
│           ├── HttpServer.java
│           ├── HttpRequest.java
│           ├── HttpResponse.java
│           ├── HttpMethod.java
│           ├── HttpStatus.java
│           ├── RequestParser.java
│           ├── ResponseWriter.java
│           ├── Router.java
│           ├── Route.java
│           ├── RouteMatch.java
│           ├── RouteHandler.java
│           ├── StaticFileHandler.java
│           ├── Middleware.java
│           ├── MiddlewareChain.java
│           ├── LoggingMiddleware.java
│           ├── TimingMiddleware.java
│           ├── ErrorHandlingMiddleware.java
│           ├── Session.java
│           ├── SessionManager.java
│           ├── SessionMiddleware.java
│           ├── ServerConfig.java
│           └── Main.java
│
└── test/
    └── java/
        └── org/example/
            └── ...
```

## Current Limitations

This project is designed as an educational HTTP server rather than a production replacement for established Java web servers.

Current limitations include:

- HTTP/1.1 only
- connection closed after each response
- no HTTPS/TLS
- no HTTP/2 or HTTP/3
- no chunked transfer encoding
- no persistent database-backed sessions
- sessions are lost when the server restarts
- limited cookie options
- limited HTTP method helpers
- limited request size/security controls
- no production-grade authentication

These constraints are intentional: the project focuses on implementing and understanding the core mechanics of an HTTP server.

## What I Learned

Building the server from the socket layer provided practical experience with:

- TCP socket programming
- HTTP/1.1 request and response structure
- protocol parsing and validation
- routing algorithms
- Java NIO file handling
- directory traversal prevention
- concurrency and thread pools
- thread-safe shared state
- graceful resource shutdown
- middleware architecture
- cookies and server-side sessions
- unit versus integration testing
- defensive error handling

The project demonstrates how higher-level web frameworks build abstractions such as routing, middleware, request objects, response objects and sessions on top of lower-level networking primitives.
