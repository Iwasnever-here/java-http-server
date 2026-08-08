package org.example;

public class ErrorHandlingMiddleware implements Middleware{
    @Override
    public void handle(HttpRequest request, HttpResponse response, MiddlewareChain chain) throws Exception {
        try{
            chain.next(request, response);

        }catch(Exception exception) {
            System.err.println("Unhandled route error: "+ exception.getMessage());

            response.status(HttpStatus.INTERNAL_SERVER_ERROR).text("Internal Server Error");
        }
    }
}
