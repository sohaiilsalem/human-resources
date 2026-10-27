package com.hrsaas.request.internal;

import static com.hrsaas.request.internal.FieldDefinition.of;
import static com.hrsaas.request.internal.FieldType.AMOUNT;
import static com.hrsaas.request.internal.FieldType.DATE;
import static com.hrsaas.request.internal.FieldType.NUMBER;
import static com.hrsaas.request.internal.FieldType.SELECT;
import static com.hrsaas.request.internal.FieldType.TEXT;
import static com.hrsaas.request.internal.FieldType.TIME;

import java.math.BigDecimal;
import java.util.List;

import com.hrsaas.request.RequestCategory;

/**
 * Request types every company starts with. Workflow codes refer to the default workflows (DefaultWorkflows in the
 * workflow module). Companies edit forms, limits and workflows, and add their own types.
 */
final class DefaultRequestTypes {

	private DefaultRequestTypes() {
	}

	static List<RequestTypeRequest> defaults() {
		FieldDefinition date = of("date", "Date", "التاريخ", DATE, true);
		FieldDefinition startDate = of("startDate", "From", "من", DATE, true);
		FieldDefinition endDate = of("endDate", "To", "إلى", DATE, true);
		FieldDefinition cost = of("cost", "Cost (EGP)", "التكلفة (جنيه)", AMOUNT, false).withRange(BigDecimal.ZERO, null);
		return List.of(
				type("EARLY_DEPARTURE", "Early departure", "إذن انصراف مبكر", RequestCategory.PERMISSION,
						"Leave work before the end of the shift.", "الانصراف قبل نهاية الوردية.",
						List.of(date, of("time", "Leaving at", "وقت الانصراف", TIME, true)), "MANAGER", true, true, 2,
						10),
				type("LATE_ARRIVAL", "Late arrival", "إذن تأخير", RequestCategory.PERMISSION,
						"Arrive after the start of the shift.", "الحضور بعد بداية الوردية.",
						List.of(date, of("time", "Arriving at", "وقت الحضور", TIME, true)), "MANAGER", true, true, 2,
						20),
				type("TEMPORARY_EXIT", "Temporary exit", "إذن خروج مؤقت", RequestCategory.PERMISSION,
						"Step out during working hours and come back.", "الخروج أثناء العمل ثم العودة.",
						List.of(date, of("fromTime", "From", "من", TIME, true), of("toTime", "To", "إلى", TIME, true)),
						"MANAGER", true, true, 2, 30),
				type("WORK_FROM_HOME", "Work from home", "العمل من المنزل", RequestCategory.REMOTE_WORK, null, null,
						List.of(startDate, endDate), "MANAGER", true, true, null, 40),
				type("MISSION", "Business mission", "مأمورية عمل", RequestCategory.TRAVEL, null, null,
						List.of(of("destination", "Destination", "الجهة", TEXT, true), startDate, endDate, cost),
						"MANAGER_HR", true, true, null, 50),
				type("LOAN", "Loan", "سلفة", RequestCategory.FINANCIAL, null, null,
						List.of(of("amount", "Amount (EGP)", "المبلغ (جنيه)", AMOUNT, true)
							.withRange(BigDecimal.ONE, null),
								of("installments", "Monthly installments", "عدد الأقساط الشهرية", NUMBER, true)
									.withRange(BigDecimal.ONE, new BigDecimal("60"))),
						"FINANCE", true, false, null, 60),
				type("SALARY_ADVANCE", "Salary advance", "سلفة على الراتب", RequestCategory.FINANCIAL,
						"Part of this month's salary paid early.", "صرف جزء من راتب الشهر مقدماً.",
						List.of(of("amount", "Amount (EGP)", "المبلغ (جنيه)", AMOUNT, true)
							.withRange(BigDecimal.ONE, null)),
						"FINANCE", true, false, 1, 70),
				type("OVERTIME_REVIEW", "Overtime payment review", "مراجعة مستحقات العمل الإضافي",
						RequestCategory.FINANCIAL, null, null,
						List.of(date, of("hours", "Hours", "عدد الساعات", NUMBER, true)
							.withRange(new BigDecimal("0.5"), new BigDecimal("24"))),
						"FINANCE", true, true, null, 80),
				type("TRAINING_COURSE", "Training course", "دورة تدريبية", RequestCategory.DEVELOPMENT, null, null,
						List.of(of("course", "Course", "الدورة", TEXT, true), of("provider", "Provider", "الجهة", TEXT, false),
								startDate, of("endDate", "To", "إلى", DATE, false), cost),
						"MANAGER_HR", true, true, null, 90),
				type("CERTIFICATION", "Certification", "شهادة مهنية", RequestCategory.DEVELOPMENT, null, null,
						List.of(of("certification", "Certification", "الشهادة", TEXT, true),
								of("provider", "Provider", "الجهة", TEXT, false),
								of("examDate", "Exam date", "تاريخ الاختبار", DATE, false), cost),
						"MANAGER_HR", true, true, null, 100),
				type("RESIGNATION", "Resignation", "استقالة", RequestCategory.LIFECYCLE, null, null,
						List.of(of("lastWorkingDay", "Last working day", "آخر يوم عمل", DATE, true)), "MANAGERS_HR",
						true, false, null, 110),
				type("TERMINATION", "Termination", "إنهاء خدمة", RequestCategory.LIFECYCLE,
						"Filed by a manager or HR, never by the employee.", "يقدمه المدير أو الموارد البشرية فقط.",
						List.of(of("lastWorkingDay", "Last working day", "آخر يوم عمل", DATE, true),
								of("grounds", "Grounds", "السبب", SELECT, true)
									.withOptions(List.of("PERFORMANCE", "MISCONDUCT", "RESTRUCTURING",
											"END_OF_CONTRACT", "OTHER"))),
						"HR_OWNER", false, true, null, 120));
	}

	private static RequestTypeRequest type(String code, String nameEn, String nameAr, RequestCategory category,
			String descriptionEn, String descriptionAr, List<FieldDefinition> fields, String workflow,
			boolean selfService, boolean allowOnBehalf, Integer maxPerMonth, int sortOrder) {
		return new RequestTypeRequest(code, nameEn, nameAr, descriptionEn, descriptionAr, category, fields, workflow,
				selfService, allowOnBehalf, maxPerMonth, true, sortOrder);
	}

}
