package com.hrsaas.request.internal;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param data form values by field key, checked against the type's fields */
record RequestInput(@NotBlank String type, Map<String, Object> data, @Size(max = 1000) String reason) {

}
