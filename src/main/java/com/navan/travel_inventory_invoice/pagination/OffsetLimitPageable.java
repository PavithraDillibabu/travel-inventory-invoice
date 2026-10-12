package com.navan.travel_inventory_invoice.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Pageable implementation that preserves exact offsets rather than page-aligned offsets. */
public record OffsetLimitPageable(long offset, int pageSize, Sort sort) implements Pageable {
    public OffsetLimitPageable {
        if (offset < 0 || pageSize < 1) {
            throw new IllegalArgumentException("Offset must be nonnegative and page size positive");
        }
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / pageSize);
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageable(Math.addExact(offset, pageSize), pageSize, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetLimitPageable(Math.max(0, offset - pageSize), pageSize, sort)
                : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageable(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        return new OffsetLimitPageable(Math.multiplyExact((long) pageNumber, pageSize), pageSize, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
