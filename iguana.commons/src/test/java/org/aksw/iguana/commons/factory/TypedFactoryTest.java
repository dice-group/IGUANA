package org.aksw.iguana.commons.factory;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TypedFactoryTest {

	@Test
	public void argumentClassesTest() {
		String[] args = new String[] { "a1", "b1" };
		String[] args2 = new String[] { "a2", "b2" };
		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject",
				new Object[] { args, args2 }, new Class[] { String[].class, String[].class });
		assertEquals(args[0], testObject.getArgs()[0]);
		assertEquals(args[1], testObject.getArgs()[1]);
		assertEquals(args2[0], testObject.getArgs2()[0]);
		assertEquals(args2[1], testObject.getArgs2()[1]);

	}

	@Test
	public void argumentStringsTest() {

		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", null);
		assertEquals("a3", testObject.getArgs()[0]);
		assertEquals("b3", testObject.getArgs()[1]);
	}

}
