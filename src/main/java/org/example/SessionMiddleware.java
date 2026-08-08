package org.example;

public class SessionMiddleware
        implements Middleware {

    private static final String COOKIE_NAME = "sessionId";

    private final SessionManager sessionManager;

    public SessionMiddleware(SessionManager sessionManager) {
        this.sessionManager =
                sessionManager;
    }

    @Override
    public void handle(
            HttpRequest request,
            HttpResponse response,
            MiddlewareChain chain
    ) throws Exception {
        String sessionId = request.getCookie(COOKIE_NAME);

        Session session = sessionManager.get(sessionId);

        if (session == null) {
            session = sessionManager.create();

            response.cookie(COOKIE_NAME, session.getId());
        }

        request.setSession(session);

        chain.next(request, response);
    }
}