package com.paginass.dto;

import java.util.List;

public record FeaturedProductsListDto(List<FeaturedProductDto> products, int maxAllowed, int maxImageMb) {
}
