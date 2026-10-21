package com.hrsaas.leave.internal;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record LeaveRequestInput(@NotBlank String leaveType, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
		boolean halfDay, @Size(max = 500) String reason) {

}
