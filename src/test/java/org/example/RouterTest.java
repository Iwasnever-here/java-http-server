package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


class RouterTest {

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Test
    void findsExactRoute() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        RouteHandler handler = router.find(
                HttpMethod.GET,
                "/users"
        );

        assertNotNull(handler);
    }

    @Test
    void distinguishesRoutesByMethod() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        RouteHandler handler = router.find(
                HttpMethod.POST,
                "/users"
        );

        assertNull(handler);
    }

    @Test
    void returnsNullWhenRouteDoesNotExist() {
        RouteHandler handler = router.find(
                HttpMethod.GET,
                "/missing"
        );

        assertNull(handler);
    }

    @Test
    void detectsPathRegisteredForAnotherMethod() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        assertTrue(router.hasPath("/users"));
    }

    @Test
    void rejectsDuplicateRouteRegistration() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> router.add(
                        HttpMethod.GET,
                        "/users",
                        (request, response) -> {
                        }
                )
        );
    }
    @Test
    void findsGetAndPostRoutesSeparately() {
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
                router.find(
                        HttpMethod.GET,
                        "/users"
                )
        );

        assertNotNull(
                router.find(
                        HttpMethod.POST,
                        "/users"
                )
        );
    }
    @Test
    void pathExistsEvenWhenMethodDoesNotMatch() {
        router.add(
                HttpMethod.GET,
                "/users",
                (request, response) -> {
                }
        );

        assertNull(
                router.find(
                        HttpMethod.DELETE,
                        "/users"
                )
        );

        assertTrue(
                router.hasPath("/users")
        );
    }
    @Test
    void pathDoesNotExist() {
        assertNull(
                router.find(
                        HttpMethod.GET,
                        "/missing"
                )
        );

        assertFalse(
                router.hasPath("/missing")
        );
    }

}