package com.hrsaas.leave.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.hrsaas.employee.EmployeeInfo;
import com.hrsaas.employee.Gender;
import com.hrsaas.leave.LeaveCategory;

/** API response shapes. */
final class LeaveViews {

	private LeaveViews() {
	}

	record TypeView(UUID id, String code, String nameEn, String nameAr, LeaveCategory category, boolean paid,
			boolean tracksBalance, String deductsFrom, DayCountMode dayCountMode, boolean allowHalfDay,
			Integer maxDaysPerRequest, Integer maxDaysPerYear, Integer maxTimesPerService, Integer minServiceMonths,
			Gender gender, boolean requiresAttachment, boolean active) {

		static TypeView of(LeaveType type) {
			return new TypeView(type.getId(), type.getCode(), type.getNameEn(), type.getNameAr(), type.getCategory(),
					type.isPaid(), type.isTracksBalance(),
					type.getDeductsFrom() != null ? type.getDeductsFrom().getCode() : null, type.getDayCountMode(),
					type.isAllowHalfDay(), type.getMaxDaysPerRequest(), type.getMaxDaysPerYear(),
					type.getMaxTimesPerService(), type.getMinServiceMonths(), type.getGender(),
					type.isRequiresAttachment(), type.isActive());
		}

	}

	/**
	 * Balance fields are set for types with their own ledger; a type that deducts from another names it in
	 * {@code deductsFrom}. {@code usedThisYear} counts approved days of this type in the current leave year.
	 */
	record BalanceView(String code, String nameEn, String nameAr, LeaveCategory category, boolean paid,
			String deductsFrom, BigDecimal entitledThisYear, BigDecimal balance, BigDecimal pending,
			BigDecimal available, BigDecimal usedThisYear, Integer maxDaysPerYear) {
	}

	record RequestView(UUID id, UUID employeeId, String employeeCode, String employeeNameEn, String employeeNameAr,
			String leaveType, String leaveTypeNameEn, String leaveTypeNameAr, LocalDate startDate, LocalDate endDate,
			boolean halfDay, BigDecimal days, LeaveRequestStatus status, String reason, UUID requestedBy,
			UUID decidedBy, Instant decidedAt, String decisionNote, Instant createdAt) {

		static RequestView of(LeaveRequest request, EmployeeInfo employee) {
			LeaveType type = request.getLeaveType();
			return new RequestView(request.getId(), request.getEmployeeId(), employee != null ? employee.code() : null,
					employee != null ? employee.fullNameEn() : null, employee != null ? employee.fullNameAr() : null,
					type.getCode(), type.getNameEn(), type.getNameAr(), request.getStartDate(), request.getEndDate(),
					request.isHalfDay(), request.getDays(), request.getStatus(), request.getReason(),
					request.getRequestedBy(), request.getDecidedBy(), request.getDecidedAt(),
					request.getDecisionNote(), request.getCreatedAt());
		}

	}

	record TransactionView(UUID id, String leaveType, int leaveYear, TransactionKind kind, BigDecimal days,
			LocalDate effectiveDate, UUID requestId, String note, Instant createdAt) {
	}

	record SettingsView(int firstYearDays, int eligibilityMonths, int standardDays, int seniorDays,
			int seniorServiceYears, int seniorAge, int disabilityDays, boolean allowNegativeBalance) {

		static SettingsView of(LeaveSettings settings) {
			EntitlementRules rules = settings.rules();
			return new SettingsView(rules.firstYearDays(), rules.eligibilityMonths(), rules.standardDays(),
					rules.seniorDays(), rules.seniorServiceYears(), rules.seniorAge(), rules.disabilityDays(),
					settings.isAllowNegativeBalance());
		}

		EntitlementRules rules() {
			return new EntitlementRules(firstYearDays, eligibilityMonths, standardDays, seniorDays, seniorServiceYears,
					seniorAge, disabilityDays);
		}

	}

}
