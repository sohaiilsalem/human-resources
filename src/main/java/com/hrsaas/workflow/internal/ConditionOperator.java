package com.hrsaas.workflow.internal;

import java.math.BigDecimal;

enum ConditionOperator {

	GT, GTE, LT, LTE, EQ;

	boolean test(BigDecimal actual, BigDecimal expected) {
		int c = actual.compareTo(expected);
		return switch (this) {
			case GT -> c > 0;
			case GTE -> c >= 0;
			case LT -> c < 0;
			case LTE -> c <= 0;
			case EQ -> c == 0;
		};
	}

}
