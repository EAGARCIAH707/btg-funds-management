package com.btg.funds.adapter.in.rest.model.dto;

import com.btg.funds.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;

public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <D, R> PaginatedResponse<R> from(PageResult<D> pageResult, Function<D, R> mapper) {
        var mapped = pageResult.content().stream().map(mapper).toList();
        return new PaginatedResponse<>(mapped, pageResult.page(), pageResult.size(),
                pageResult.totalElements(), pageResult.totalPages());
    }
}
