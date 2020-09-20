package org.aksw.iguana.commons.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
	public void noConstructor(){
		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		HashMap<Object, Object> map = new HashMap<Object, Object>();
		map.put("nope", "nope");
		assertEquals(null, factory.create("org.aksw.iguana.commons.factory.FactorizedObject", map));
		assertEquals(null, factory.create("org.aksw.iguana.commons.factory.FactorizedObject", new Object[]{"nope"}));
		assertEquals(null, factory.create("org.aksw.iguana.commons.factory.FactorizedObject", new Object[]{"nope"}, new Class<?>[]{String.class}));
		assertEquals(null, factory.createAnnotated("org.aksw.iguana.commons.factory.AnnotatedFactorizedObject", map));

		map.clear();
		map.put("a", 123);
		map.put("b", true);
		assertEquals(null, factory.create("org.aksw.iguana.commons.factory.FactorizedObject", map));
		assertEquals(null, factory.createAnnotated("org.aksw.iguana.commons.factory.AnnotatedFactorizedObject", map));

	}

	@Test
	public void nullConstructorClass(){
		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", new Object[]{"a", "b", "c"}, (Class[])null);
		assertEquals("a", testObject.getArgs()[0]);
		assertEquals("b", testObject.getArgs()[1]);
		assertEquals("c", testObject.getArgs()[2]);
		testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", (Object[])null, (Class[])null);
		assertEquals("a3", testObject.getArgs()[0]);
		assertEquals("b3", testObject.getArgs()[1]);
	}

	@Test
	public void nullClass(){
		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		assertEquals(null, factory.create(null, new HashMap<>()));
		assertEquals(null, factory.create(null, new Object[]{}));
		assertEquals(null, factory.create(null, new Object[]{}, new Class<?>[]{}));

	}

	@Test
	public void classNameNotFoundTest(){
		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		assertEquals(null, factory.create("thisClassShouldNotExist", new HashMap<>()));
		assertEquals(null, factory.create("thisClassShouldNotExist", new Object[]{}));
		assertEquals(null, factory.create("thisClassShouldNotExist", new Object[]{}, new Class<?>[]{}));
		assertEquals(null, factory.createAnnotated("thisClassShouldNotExist", new HashMap<>()));
	}

	@Test
	public void argumentStringsTest() {

		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", (Object[])null);
		assertEquals("a3", testObject.getArgs()[0]);
		assertEquals("b3", testObject.getArgs()[1]);
	}


	@Test
	public void mapCreationTestParameterNames() {

		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		Map arguments = new HashMap();
		arguments.put("a", "a4");
		arguments.put("b", "b4");
		arguments.put("c", "c4");
		FactorizedObject testObject = factory.createAnnotated("org.aksw.iguana.commons.factory.AnnotatedFactorizedObject", arguments);
		assertEquals("a4", testObject.getArgs()[0]);
		assertEquals("b4", testObject.getArgs()[1]);
		assertEquals("c4", testObject.getArgs()[2]);
		arguments.clear();
		arguments.put("a", "a5");
		testObject = factory.createAnnotated("org.aksw.iguana.commons.factory.AnnotatedFactorizedObject", arguments);
		assertEquals("a5", testObject.getArgs()[0]);
		assertEquals("wasNull", testObject.getArgs()[1]);
	}

	@Test
	public void testNullable() {

		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		Map arguments = new HashMap();
		arguments.put("a", "a4");
		arguments.put("b", "b4");
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", arguments);
		assertEquals("a4", testObject.getArgs()[0]);
		assertEquals("b4", testObject.getArgs()[1]);
		arguments.remove("b");
		testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", arguments);
		assertEquals("a4", testObject.getArgs()[0]);
		assertEquals("wasNull", testObject.getArgs()[1]);

	}

	@Test
	public void mapCreationTest() {

		TypedFactory<FactorizedObject> factory = new TypedFactory<FactorizedObject>();
		Map arguments = new HashMap();
		arguments.put("a", "a4");
		arguments.put("b", "b4");
		arguments.put("c", "c4");
		FactorizedObject testObject = factory.create("org.aksw.iguana.commons.factory.FactorizedObject", arguments);
		assertEquals("a4", testObject.getArgs()[0]);
		assertEquals("b4", testObject.getArgs()[1]);
		assertEquals("c4", testObject.getArgs()[2]);

	}

	@Test
	public void shortHandAnnotationTest() {

		TypedFactory<AnnotatedFactorizedObject> factory = new TypedFactory<AnnotatedFactorizedObject>();
		Map arguments = new HashMap();
		arguments.put("a", "a4");
		arguments.put("b", "b4");
		arguments.put("c", "c4");
		AnnotatedFactorizedObject testObject = factory.create("facto", arguments);
		assertEquals("a4", testObject.getArgs()[0]);
		assertEquals("b4", testObject.getArgs()[1]);
		assertEquals("c4", testObject.getArgs()[2]);

	}

}
