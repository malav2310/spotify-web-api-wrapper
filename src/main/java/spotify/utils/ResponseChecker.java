package spotify.utils;

import com.google.gson.Gson;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import spotify.api.enums.HttpStatusCode;
import spotify.exceptions.SpotifyActionFailedException;
import spotify.models.errors.SpotifyError;

import java.util.HashMap;
import java.util.Map;

public class ResponseChecker {
    
    private static final Map<Integer, ResponseHandler> handlers = new HashMap<>();

    static {
        handlers.put(200, new SuccessHandler());
        handlers.put(400, new BadRequestHandler());
        handlers.put(401, new UnauthorizedHandler());
        handlers.put(500, new ServerErrorHandler());
    }

    public static <T> void throwIfRequestHasNotBeenFulfilledCorrectly(final Response<T> response, final HttpStatusCode expectedStatusCode) {
        int statusCode = response.code();
        ResponseHandler handler = handlers.getOrDefault(statusCode, new DefaultErrorHandler());
        handler.handle(response);
    }
}

// Interface for handling responses
interface ResponseHandler {
    <T> void handle(Response<T> response);
}

// Success Handler
class SuccessHandler implements ResponseHandler {
    public <T> void handle(Response<T> response) {
        // No action needed for successful requests
    }
}

// Bad Request Handler
class BadRequestHandler implements ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(BadRequestHandler.class);

    public <T> void handle(Response<T> response) {
        logger.warn("Bad Request: {}", response.message());
        throw new SpotifyActionFailedException("Bad Request: " + response.message());
    }
}

// Unauthorized Handler
class UnauthorizedHandler implements ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(UnauthorizedHandler.class);

    public <T> void handle(Response<T> response) {
        logger.error("Unauthorized Access: {}", response.message());
        throw new SpotifyActionFailedException("Unauthorized: " + response.message());
    }
}

// Server Error Handler
class ServerErrorHandler implements ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerErrorHandler.class);

    public <T> void handle(Response<T> response) {
        logger.error("Internal Server Error: {}", response.message());
        throw new SpotifyActionFailedException("Server Error: " + response.message());
    }
}

// Default Error Handler
class DefaultErrorHandler implements ResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);
    private final Gson gson = new Gson();

    public <T> void handle(Response<T> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            SpotifyError spotifyError = gson.fromJson(errorBody.charStream(), SpotifyError.class);
            if (spotifyError != null) {
                logger.error("Spotify API Error: {} - {}", spotifyError.getError().getStatus(), spotifyError.getError().getMessage());
                throw new SpotifyActionFailedException(spotifyError.getError().getMessage());
            }
        }
        throw new SpotifyActionFailedException("Unknown error occurred.");
    }
}
