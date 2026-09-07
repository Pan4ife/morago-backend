package org.morago.util;

import org.morago.exception.InvalidPaginationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PaginationValidator {

    private static final int MAX_SIZE = 100;

    private PaginationValidator() {
    }

    public static Pageable validate(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("Page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPaginationException("Size must be between 1 and " + MAX_SIZE);
        }
        return PageRequest.of(page, size);
    }
}