package com.hrsaas.leave.internal;

import static com.hrsaas.leave.internal.DayCountMode.CALENDAR_DAYS;
import static com.hrsaas.leave.internal.DayCountMode.WORKING_DAYS;

import java.util.List;

import com.hrsaas.employee.Gender;
import com.hrsaas.leave.LeaveCategory;

/**
 * Leave types seeded for every new tenant, per Labour Law No. 14 of 2025 and Social Insurance Law No. 148 of 2019.
 * Tenants can edit names and limits and add their own types.
 *
 * <p>
 * ⚠️ Limits to be confirmed by an Egyptian labour-law advisor before go-live (ARCHITECTURE §7, §13.1). Pay rates for
 * sick leave (75% / 85% per the Social Insurance Law) are applied by payroll, not here.
 */
final class EgyptianLeaveTypes {

	static final String ANNUAL = "ANNUAL";

	private EgyptianLeaveTypes() {
	}

	/** In dependency order: a type that deducts from another comes after it. */
	static List<LeaveTypeRequest> defaults() {
		return List.of(
				// days from EntitlementRules; rest days and holidays inside the leave are not counted
				new LeaveTypeRequest(ANNUAL, "Annual leave", "إجازة اعتيادية", LeaveCategory.ANNUAL, true, true, null,
						WORKING_DAYS, true, null, null, null, null, null, false, true),
				// unplanned absence, counted within annual leave
				new LeaveTypeRequest("CASUAL", "Casual leave", "إجازة عارضة", LeaveCategory.CASUAL, true, true, ANNUAL,
						WORKING_DAYS, false, 2, 7, null, null, null, false, true),
				new LeaveTypeRequest("SICK", "Sick leave", "إجازة مرضية", LeaveCategory.SICK, true, false, null,
						CALENDAR_DAYS, false, null, null, null, null, null, true, true),
				new LeaveTypeRequest("MATERNITY", "Maternity leave", "إجازة وضع", LeaveCategory.MATERNITY, true, false,
						null, CALENDAR_DAYS, false, 120, null, 3, null, Gender.FEMALE, true, true),
				new LeaveTypeRequest("HAJJ", "Hajj leave", "إجازة الحج", LeaveCategory.HAJJ, true, false, null,
						CALENDAR_DAYS, false, 30, null, 1, 60, null, false, true),
				new LeaveTypeRequest("UNPAID", "Unpaid leave", "إجازة بدون أجر", LeaveCategory.UNPAID, false, false,
						null, WORKING_DAYS, false, null, null, null, null, null, false, true));
	}

}
