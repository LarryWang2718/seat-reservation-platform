package com.project.seat_reserve.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.InvalidHoldStateException;
import com.project.seat_reserve.common.exception.OrderCleanupFailedException;
import com.project.seat_reserve.hold.HoldStatus;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;

class GraphQlExceptionResolverTest {
    private GraphQlExceptionResolver resolver;
    private DataFetchingEnvironment environment;

    @BeforeEach
    void setUp() {
        resolver = new GraphQlExceptionResolver();
        environment = mock(DataFetchingEnvironment.class);
        when(environment.getField()).thenReturn(Field.newField("confirmOrder").build());
        when(environment.getExecutionStepInfo()).thenReturn(ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.parse("/confirmOrder"))
            .build());
    }

    @Test
    void mapsNotFoundExceptionsToNotFoundError() {
        GraphQLError error = resolver.resolveToSingleError(new EventNotFoundException(999L), environment);

        assertError(error, "Event not found: 999", "NOT_FOUND", "EventNotFoundException");
    }

    @Test
    void mapsDomainValidationExceptionsToBadRequestError() {
        GraphQLError error = resolver.resolveToSingleError(new InvalidHoldStateException(100L, HoldStatus.EXPIRED), environment);

        assertError(error, "Hold 100 is in an invalid state: EXPIRED", "BAD_REQUEST", "InvalidHoldStateException");
    }

    @Test
    void mapsDataIntegrityViolationsToGenericBadRequestError() {
        GraphQLError error = resolver.resolveToSingleError(new DataIntegrityViolationException("duplicate key"), environment);

        assertError(error, "Request violates a data integrity constraint", "BAD_REQUEST", "DataIntegrityViolationException");
    }

    @Test
    void exposesCleanupFailureMessageAsInternalError() {
        GraphQLError error = resolver.resolveToSingleError(
            new OrderCleanupFailedException(42L, new RuntimeException("confirm failed"), new RuntimeException("cleanup failed")),
            environment
        );

        assertError(error, "Order 42 failed confirmation and cleanup also failed; order remains pending", "INTERNAL_ERROR", "OrderCleanupFailedException");
    }

    @Test
    void sanitizesUnexpectedExceptions() {
        GraphQLError error = resolver.resolveToSingleError(new RuntimeException("boom"), environment);

        assertError(error, "Internal server error", "INTERNAL_ERROR", "RuntimeException");
    }

    @SuppressWarnings("unchecked")
    private void assertError(GraphQLError error, String message, String classification, String code) {
        assertThat(error.getMessage()).isEqualTo(message);

        Map<String, Object> specification = error.toSpecification();
        Map<String, Object> extensions = (Map<String, Object>) specification.get("extensions");

        assertThat(extensions.get("classification")).isEqualTo(classification);
        assertThat(extensions.get("code")).isEqualTo(code);
    }
}
