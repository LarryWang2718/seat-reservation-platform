package com.project.seat_reserve.graphql;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.project.seat_reserve.common.exception.ActiveOrderAlreadyExistsException;
import com.project.seat_reserve.common.exception.EventNotFoundException;
import com.project.seat_reserve.common.exception.EventNotOpenForOrderingException;
import com.project.seat_reserve.common.exception.EventSaleWindowClosedException;
import com.project.seat_reserve.common.exception.HoldLimitExceededException;
import com.project.seat_reserve.common.exception.InvalidEventTimeRangeException;
import com.project.seat_reserve.common.exception.InvalidHoldState;
import com.project.seat_reserve.common.exception.InvalidSaleWindowException;
import com.project.seat_reserve.common.exception.InvalidSessionIdException;
import com.project.seat_reserve.common.exception.NoActiveHoldsForOrderException;
import com.project.seat_reserve.common.exception.OrderNotFoundException;
import com.project.seat_reserve.common.exception.OrderNotPendingException;
import com.project.seat_reserve.common.exception.SaleWindowAfterEventStartException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldByOrderException;
import com.project.seat_reserve.common.exception.SeatAlreadyHeldException;
import com.project.seat_reserve.common.exception.SeatAlreadySoldException;
import com.project.seat_reserve.common.exception.SeatNotFoundException;
import com.project.seat_reserve.common.exception.SeatOrderMismatchException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;

@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (isNotFound(ex)) {
            return buildError(env, ex.getMessage(), ErrorType.NOT_FOUND, ex.getClass().getSimpleName());
        }
        if (isBadRequest(ex)) {
            return buildError(env, ex.getMessage(), ErrorType.BAD_REQUEST, ex.getClass().getSimpleName());
        }
        if (ex instanceof ConstraintViolationException constraintViolationException) {
            return buildError(env, buildConstraintViolationMessage(constraintViolationException), ErrorType.BAD_REQUEST,
                constraintViolationException.getClass().getSimpleName());
        }
        if (ex instanceof BindException bindException) {
            return buildError(env, bindException.getAllErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.toString())
                .collect(Collectors.joining(", ")), ErrorType.BAD_REQUEST, bindException.getClass().getSimpleName());
        }
        if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return buildError(env, methodArgumentNotValidException.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.toString())
                .collect(Collectors.joining(", ")), ErrorType.BAD_REQUEST, methodArgumentNotValidException.getClass().getSimpleName());
        }
        if (ex instanceof DateTimeParseException) {
            return buildError(env, "Invalid date-time format", ErrorType.BAD_REQUEST, ex.getClass().getSimpleName());
        }
        if (ex instanceof DataIntegrityViolationException) {
            return buildError(env, "Request violates a data integrity constraint", ErrorType.BAD_REQUEST,
                ex.getClass().getSimpleName());
        }

        return buildError(env, "Internal server error", ErrorType.INTERNAL_ERROR, ex.getClass().getSimpleName());
    }

    private boolean isNotFound(Throwable ex) {
        return ex instanceof EventNotFoundException
            || ex instanceof OrderNotFoundException
            || ex instanceof SeatNotFoundException;
    }

    private boolean isBadRequest(Throwable ex) {
        return ex instanceof ActiveOrderAlreadyExistsException
            || ex instanceof EventNotOpenForOrderingException
            || ex instanceof EventSaleWindowClosedException
            || ex instanceof HoldLimitExceededException
            || ex instanceof InvalidEventTimeRangeException
            || ex instanceof InvalidHoldState
            || ex instanceof InvalidSaleWindowException
            || ex instanceof InvalidSessionIdException
            || ex instanceof NoActiveHoldsForOrderException
            || ex instanceof OrderNotPendingException
            || ex instanceof SaleWindowAfterEventStartException
            || ex instanceof SeatAlreadyHeldByOrderException
            || ex instanceof SeatAlreadyHeldException
            || ex instanceof SeatAlreadySoldException
            || ex instanceof SeatOrderMismatchException;
    }

    private String buildConstraintViolationMessage(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .collect(Collectors.joining(", "));
    }

    private GraphQLError buildError(DataFetchingEnvironment env, String message, ErrorType errorType, String code) {
        return GraphqlErrorBuilder.newError(env)
            .message(message)
            .errorType(errorType)
            .extensions(Map.of("code", code))
            .build();
    }
}
