package com.poe2craft.admin.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 브라우저 이벤트의 kind는 허용된 이벤트 이름이며 count는 합산한 발생 횟수다. */
public record Event(
    @NotNull
        @Pattern(
            regexp =
                "view_workbench|view_history|runtime_error|promise_error|history_save|catalog_load")
        String kind,
    @Min(1) @Max(100) int count) {}
