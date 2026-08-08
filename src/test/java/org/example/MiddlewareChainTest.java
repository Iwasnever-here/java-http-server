package org.example;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MiddlewareChainTest {

    @Test
    void runsHandlerWhenThereIsNoMiddleware()
            throws Exception {
        List<String> events = new ArrayList<>();

        RouteHandler handler =
                (request, response) -> {
                    events.add("handler");
                    response.text("OK");
                };

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(),
                        handler
                );

        HttpResponse response =
                new HttpResponse();

        chain.next(
                createRequest("/test"),
                response
        );

        assertEquals(
                List.of("handler"),
                events
        );

        assertEquals(
                "OK",
                bodyAsString(response)
        );
    }

    @Test
    void middlewareRunsBeforeAndAfterHandler()
            throws Exception {
        List<String> events = new ArrayList<>();

        Middleware middleware =
                (request, response, chain) -> {
                    events.add("before");

                    chain.next(
                            request,
                            response
                    );

                    events.add("after");
                };

        RouteHandler handler =
                (request, response) ->
                        events.add("handler");

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/test"),
                new HttpResponse()
        );

        assertEquals(
                List.of(
                        "before",
                        "handler",
                        "after"
                ),
                events
        );
    }

    @Test
    void multipleMiddlewareRunsInNestedOrder()
            throws Exception {
        List<String> events = new ArrayList<>();

        Middleware first =
                (request, response, chain) -> {
                    events.add("first-before");

                    chain.next(
                            request,
                            response
                    );

                    events.add("first-after");
                };

        Middleware second =
                (request, response, chain) -> {
                    events.add("second-before");

                    chain.next(
                            request,
                            response
                    );

                    events.add("second-after");
                };

        RouteHandler handler =
                (request, response) ->
                        events.add("handler");

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(
                                first,
                                second
                        ),
                        handler
                );

        chain.next(
                createRequest("/test"),
                new HttpResponse()
        );

        assertEquals(
                List.of(
                        "first-before",
                        "second-before",
                        "handler",
                        "second-after",
                        "first-after"
                ),
                events
        );
    }

    @Test
    void middlewareCanModifyResponseBeforeHandler()
            throws Exception {
        Middleware middleware =
                (request, response, chain) -> {
                    response.header(
                            "X-Before",
                            "true"
                    );

                    chain.next(
                            request,
                            response
                    );
                };

        RouteHandler handler =
                (request, response) ->
                        response.text("Hello");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/test"),
                response
        );

        assertEquals(
                "true",
                response
                        .getHeaders()
                        .get("X-Before")
        );

        assertEquals(
                "Hello",
                bodyAsString(response)
        );
    }

    @Test
    void middlewareCanModifyResponseAfterHandler()
            throws Exception {
        Middleware middleware =
                (request, response, chain) -> {
                    chain.next(
                            request,
                            response
                    );

                    response.header(
                            "X-After",
                            "true"
                    );
                };

        RouteHandler handler =
                (request, response) ->
                        response.text("Hello");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/test"),
                response
        );

        assertEquals(
                "true",
                response
                        .getHeaders()
                        .get("X-After")
        );
    }

    @Test
    void middlewareCanStopChain()
            throws Exception {
        List<String> events = new ArrayList<>();

        Middleware blockingMiddleware =
                (request, response, chain) -> {
                    events.add("blocked");

                    response
                            .status(
                                    HttpStatus.FORBIDDEN
                            )
                            .text("Forbidden");

                    // Deliberately do not call chain.next()
                };

        RouteHandler handler =
                (request, response) ->
                        events.add("handler");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(blockingMiddleware),
                        handler
                );

        chain.next(
                createRequest("/private"),
                response
        );

        assertEquals(
                List.of("blocked"),
                events
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatus()
        );

        assertEquals(
                "Forbidden",
                bodyAsString(response)
        );
    }

    @Test
    void timingMiddlewareAddsResponseTimeHeader()
            throws Exception {
        TimingMiddleware middleware =
                new TimingMiddleware();

        RouteHandler handler =
                (request, response) ->
                        response.text("OK");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/test"),
                response
        );

        String responseTime =
                response
                        .getHeaders()
                        .get("X-Response-Time");

        assertNotNull(responseTime);
    }

    @Test
    void timingMiddlewareUsesMilliseconds()
            throws Exception {
        TimingMiddleware middleware =
                new TimingMiddleware();

        RouteHandler handler =
                (request, response) ->
                        response.text("OK");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/test"),
                response
        );

        String responseTime =
                response
                        .getHeaders()
                        .get("X-Response-Time");

        assertNotNull(responseTime);

        assertEquals(
                true,
                responseTime.endsWith("ms")
        );
    }

    @Test
    void errorMiddlewareConvertsExceptionTo500()
            throws Exception {
        ErrorHandlingMiddleware middleware =
                new ErrorHandlingMiddleware();

        RouteHandler handler =
                (request, response) -> {
                    throw new RuntimeException(
                            "Something exploded"
                    );
                };

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/boom"),
                response
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatus()
        );

        assertEquals(
                "Internal Server Error",
                bodyAsString(response)
        );
    }

    @Test
    void errorMiddlewareAllowsSuccessfulRequestThrough()
            throws Exception {
        ErrorHandlingMiddleware middleware =
                new ErrorHandlingMiddleware();

        RouteHandler handler =
                (request, response) ->
                        response.text("Success");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/success"),
                response
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatus()
        );

        assertEquals(
                "Success",
                bodyAsString(response)
        );
    }

    @Test
    void errorMiddlewareCanWrapOtherMiddleware()
            throws Exception {
        Middleware errorMiddleware =
                new ErrorHandlingMiddleware();

        Middleware explodingMiddleware =
                (request, response, chain) -> {
                    throw new RuntimeException(
                            "Middleware failed"
                    );
                };

        RouteHandler handler =
                (request, response) ->
                        response.text("Should not run");

        HttpResponse response =
                new HttpResponse();

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(
                                errorMiddleware,
                                explodingMiddleware
                        ),
                        handler
                );

        chain.next(
                createRequest("/test"),
                response
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatus()
        );

        assertEquals(
                "Internal Server Error",
                bodyAsString(response)
        );
    }

    @Test
    void middlewareCanInspectRequest()
            throws Exception {
        List<String> paths = new ArrayList<>();

        Middleware middleware =
                (request, response, chain) -> {
                    paths.add(request.getPath());

                    chain.next(
                            request,
                            response
                    );
                };

        RouteHandler handler =
                (request, response) ->
                        response.text("OK");

        MiddlewareChain chain =
                new MiddlewareChain(
                        List.of(middleware),
                        handler
                );

        chain.next(
                createRequest("/users/42"),
                new HttpResponse()
        );

        assertEquals(
                List.of("/users/42"),
                paths
        );
    }

    private HttpRequest createRequest(
            String path
    ) {
        return new HttpRequest(
                HttpMethod.GET,
                path,
                "HTTP/1.1",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                new byte[0]
        );
    }

    private String bodyAsString(
            HttpResponse response
    ) {
        return new String(
                response.getBody(),
                StandardCharsets.UTF_8
        );
    }
}