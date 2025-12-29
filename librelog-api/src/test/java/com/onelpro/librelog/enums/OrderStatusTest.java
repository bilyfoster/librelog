package com.onelpro.librelog.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

	@Test
	void values_When_EnumIsCalled_Expect_AllEnumValuesReturned() {
		OrderStatus[] values = OrderStatus.values();
		assertEquals(7, values.length, "OrderStatus should have exactly 7 values");
		assertTrue(values[0] == OrderStatus.DRAFT);
		assertTrue(values[1] == OrderStatus.PENDING);
		assertTrue(values[2] == OrderStatus.APPROVED);
		assertTrue(values[3] == OrderStatus.SCHEDULING);
		assertTrue(values[4] == OrderStatus.SCHEDULED);
		assertTrue(values[5] == OrderStatus.CANCELLED);
		assertTrue(values[6] == OrderStatus.COMPLETED);
	}

	@Test
	void valueOf_When_ValidEnumNameProvided_Expect_CorrectEnumReturned() {
		assertEquals(OrderStatus.DRAFT, OrderStatus.valueOf("DRAFT"));
		assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
		assertEquals(OrderStatus.APPROVED, OrderStatus.valueOf("APPROVED"));
		assertEquals(OrderStatus.SCHEDULING, OrderStatus.valueOf("SCHEDULING"));
		assertEquals(OrderStatus.SCHEDULED, OrderStatus.valueOf("SCHEDULED"));
		assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
		assertEquals(OrderStatus.COMPLETED, OrderStatus.valueOf("COMPLETED"));
	}

}

