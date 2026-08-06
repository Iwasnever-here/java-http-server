package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Test
    void findsExactRoute() {
        RouteHandler handler = (request, response) -> {
        };

        router.add(
                HttpMethod.GET,
                "/users",
                handler
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/users"
        );

        assertNotNull(match);
        assertSame(
                handler,
                match.getRoute().getHandler()
        );
        assertTrue(
                match.getPathParameters().isEmpty()
        );
    }

    @Test
    void distinguishesRoutesByMethod() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.POST,
                "/users"
        );

        assertNull(match);
        assertTrue(router.hasPath("/users"));
    }

    @Test
    void returnsNullWhenRouteDoesNotExist() {
        RouteMatch match = router.match(
                HttpMethod.GET,
                "/missing"
        );

        assertNull(match);
        assertFalse(router.hasPath("/missing"));
    }

    @Test
    void rejectsDuplicateExactRoute() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> router.add(
                        HttpMethod.GET,
                        "/users",
                        (request, response) -> {
                        }
                )
        );

        assertEquals(
                "Route already registered: GET /users",
                exception.getMessage()
        );
    }

    @Test
    void allowsSamePathForDifferentMethods() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        router.add(
                HttpMethod.POST,
                "/users",
                (request, response) -> {
                }
        );

        assertNotNull(
                router.match(
                        HttpMethod.GET,
                        "/users"
                )
        );

        assertNotNull(
                router.match(
                        HttpMethod.POST,
                        "/users"
                )
        );
    }

    @Test
    void matchesOnePathParameter() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/users/42"
        );

        assertNotNull(match);
        assertEquals(
                "42",
                match.getPathParameters().get("id")
        );
    }

    @Test
    void matchesMultiplePathParameters() {
        router.add(
                HttpMethod.GET,
                "/books/:bookId/chapters/:chapterId",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/books/7/chapters/3"
        );

        assertNotNull(match);
        assertEquals(
                "7",
                match.getPathParameters().get("bookId")
        );
        assertEquals(
                "3",
                match.getPathParameters().get("chapterId")
        );
    }

    @Test
    void requiresStaticSegmentsToMatch() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/books/42"
        );

        assertNull(match);
    }

    @Test
    void rejectsSegmentCountMismatch() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        assertNull(
                router.match(
                        HttpMethod.GET,
                        "/users"
                )
        );

        assertNull(
                router.match(
                        HttpMethod.GET,
                        "/users/42/posts"
                )
        );
    }

    @Test
    void exactRouteIsPreferredOverDynamicRoute() {
        RouteHandler exactHandler =
                (request, response) ->
                        response.text("New user");

        RouteHandler dynamicHandler =
                (request, response) ->
                        response.text("User by ID");

        router.add(
                HttpMethod.GET,
                "/users/:id",
                dynamicHandler
        );

        router.add(
                HttpMethod.GET,
                "/users/new",
                exactHandler
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/users/new"
        );

        assertNotNull(match);
        assertSame(
                exactHandler,
                match.getRoute().getHandler()
        );
        assertTrue(
                match.getPathParameters().isEmpty()
        );
    }

    @Test
    void dynamicRouteIsMethodSpecific() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.POST,
                "/users/42"
        );

        assertNull(match);
        assertTrue(router.hasPath("/users/42"));
    }

    @Test
    void rejectsDuplicateDynamicRoute() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> router.add(
                        HttpMethod.GET,
                        "/users/:id",
                        (request, response) -> {
                        }
                )
        );

        assertEquals(
                "Route already registered: GET /users/:id",
                exception.getMessage()
        );
    }

    @Test
    void allowsSameDynamicTemplateForDifferentMethods() {
        router.add(
                HttpMethod.GET,
                "/users/:id",
                (request, response) -> {
                }
        );

        router.add(
                HttpMethod.DELETE,
                "/users/:id",
                (request, response) -> {
                }
        );

        assertNotNull(
                router.match(
                        HttpMethod.GET,
                        "/users/42"
                )
        );

        assertNotNull(
                router.match(
                        HttpMethod.DELETE,
                        "/users/42"
                )
        );
    }

    @Test
    void matchesRootExactRoute() {
        router.add(
                HttpMethod.GET,
                "/",
                (request, response) -> {
                }
        );

        RouteMatch match = router.match(
                HttpMethod.GET,
                "/"
        );

        assertNotNull(match);
        assertTrue(
                match.getPathParameters().isEmpty()
        );
    }

    @Test
    void detectsDynamicPathForWrongMethodResponse() {
        router.add(
                HttpMethod.GET,
                "/books/:bookId",
                (request, response) -> {
                }
        );

        assertTrue(router.hasPath("/books/15"));
        assertFalse(router.hasPath("/users/15"));
    }
}