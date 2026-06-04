package com.inholland.banking_app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadataDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
