package com.alexsmaliy.yesimdb.logging;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.concurrent.ThreadLocalRandom;

@Provider
public class InvalidDefinitionExceptionMapper implements ExceptionMapper<InvalidDefinitionException> {
    @Override
    public Response toResponse(InvalidDefinitionException e) {
        // Analogous to LoggingExceptionMapper in DW, but without the opinionated conversion logic.
        long errorId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        DefaultLoggers.REQUEST_ERROR_LOGGER.error(
            "Problem deserializing something. (message: {}) (tracking id: {})",
            e.getMessage(),
            errorId);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Unable to interpret request. Cause: " + e.getMessage())
            .type(MediaType.TEXT_PLAIN_TYPE)
            .build();
    }
}
