/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.cesecore.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests LookAheadObjectInputStream class that can be used to prevent java deserialization issue
 * 
 * @version $Id: LookAheadObjectInputStreamTest.java 22806 2016-02-19 18:58:27Z marko $
 */
public class LookAheadObjectInputStreamTest {

    private static final Logger log = Logger.getLogger(LookAheadObjectInputStreamTest.class);

    @Before
    public void setup() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    private static class ExploitClass implements Serializable {
        private static final long serialVersionUID = 1L;

        private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            throw new IllegalStateException("Run exploit code...");
        }
    }

    private static class GoodClass1 implements Serializable {
        private static final long serialVersionUID = 2L;
        private int data = 0;

        public GoodClass1(int data) {
            this.data = data;
        }

        @SuppressWarnings("unused")
        public int getData() {
            return data;
        }
    }

    private static class GoodClass2 implements Serializable {
        private static final long serialVersionUID = 3L;
        private int data = 0;

        public GoodClass2(int data) {
            this.data = data;
        }

        public int getData() {
            return data;
        }
    }

    private static abstract class GoodAbstractClass implements Serializable {
        private static final long serialVersionUID = 2L;
    }

    private static class GoodExtendedClass extends GoodAbstractClass {
        private static final long serialVersionUID = 5L;
    }

    private static class GoodExtendedExtendedClass extends GoodExtendedClass {
        private static final long serialVersionUID = 6L;
    }

    /**
     * Test that accepted java objects can be deserialized
     */
    @Test
    public void testDeserializingAcceptedJavaObject() throws Exception {
        log.trace(">testDeserializingAcceptedJavaObject");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new GoodClass2(2));
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            lookAheadObjectInputStream.setAcceptedClasses(Arrays.asList(GoodClass1.class, GoodClass2.class));
            lookAheadObjectInputStream.setMaxObjects(1);
            GoodClass2 goodClass = (GoodClass2) lookAheadObjectInputStream.readObject();
            assertTrue("Data corrupted during testDeserializingAcceptedJavaObject", goodClass.getData() == 2);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingAcceptedJavaObject");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingAcceptedJavaObject");
    }

    /**
     * Test that non-accepted java objects can NOT be deserialized (SecurityException has to be thrown)
     */
    @Test
    public void testDeserializingNonAcceptedJavaObject() throws Exception {
        log.trace(">testDeserializingNonAcceptedJavaObject");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new ExploitClass());
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodClass1.class);
            acceptedClasses.add(GoodClass2.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            @SuppressWarnings("unused")
            GoodClass2[] deserialized = (GoodClass2[]) lookAheadObjectInputStream.readObject();
        } catch (IllegalStateException e) {
            fail("ExploitClass code was not caught with LookAheadSerializer");
        } catch (SecurityException e) {
            //Good
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingNonAcceptedJavaObject");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingNonAcceptedJavaObject");
    }

    /**
     * Test that non-initialized LookAheadObjectInputStream can not read any objects (except default (primitive) ones)
     */
    @Test
    public void testNonInitializedLookAheadObjectInputStream() throws Exception {
        log.trace(">testNonInitializedLookAheadObjectInputStream");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new ExploitClass());
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            @SuppressWarnings("unused")
            GoodClass2[] deserialized = (GoodClass2[]) lookAheadObjectInputStream.readObject();
        } catch (IllegalStateException e) {
            fail("ExploitClass code was not caught with LookAheadObjectInputStream");
        } catch (SecurityException e) {
            //Good
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testNonInitializedLookAheadObjectInputStream");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testNonInitializedLookAheadObjectInputStream");
    }

    /**
     * Test that array of accepted java objects can be deserialized
     */
    @Test
    public void testDeserializingAcceptedJavaObjectArray() throws Exception {
        log.trace(">testDeserializingAcceptedJavaObjectArray");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new GoodClass2[3]);
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodClass1.class);
            acceptedClasses.add(GoodClass2.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            GoodClass2[] deserialized = (GoodClass2[]) lookAheadObjectInputStream.readObject();
            assertTrue("Data corrupted during testDeserializingAcceptedJavaObjectArray", deserialized.length == 3);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingAcceptedJavaObjectArray");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingAcceptedJavaObjectArray");
    }

    /**
     * Test that array of non-accepted java objects can NOT be deserialized (SecurityException has to be thrown).
     * Although deserialization of non-accepted class is not exploit by itself, it seems natural to not allow it.
     */
    @Test
    public void testDeserializingNonAcceptedJavaObjectArray() throws Exception {
        log.trace(">testDeserializingNonAcceptedJavaObjectArray");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new ExploitClass[3]);
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodClass1.class);
            acceptedClasses.add(GoodClass2.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            @SuppressWarnings("unused")
            GoodClass2[] deserialized = (GoodClass2[]) lookAheadObjectInputStream.readObject();
        } catch (IllegalStateException e) {
            fail("ExploitClass code was not caught with LookAheadObjectInputStream");
        } catch (SecurityException e) {
            //Good
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingNonAcceptedJavaObjectArray");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingNonAcceptedJavaObjectArray");
    }

    /**
     * Test that array of mixed (accepted and non-accepted) objects can NOT be deserialized
     */
    @Test
    public void testDeserializingMixedObjectArray() throws Exception {
        log.trace(">testDeserializingMixedObjectArray");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            Object[] mixedObjects = new Object[3];
            mixedObjects[0] = "Dummy string";
            mixedObjects[1] = new ExploitClass();
            mixedObjects[2] = new GoodClass1(1);
            o.writeObject(mixedObjects);
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodClass1.class);
            acceptedClasses.add(GoodClass2.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            @SuppressWarnings("unused")
            Object[] deserialized = (Object[]) lookAheadObjectInputStream.readObject();
            fail("ExploitClass code was not caught with LookAheadObjectInputStream during testDeserializingMixedObjectArray");
        } catch (SecurityException e) {
            //Good
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingMixedObjectArray");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingMixedObjectArray");
    }

    /**
     * Test limiting maximum count of objects that can be deserialized
     */
    @Test
    public void testLimitingMaxObjects() throws Exception {
        log.trace(">testLimitingMaxObjects");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new GoodClass1(1));
            o.writeObject(new GoodClass1(2));
            o.writeObject(new GoodClass2(3));
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodClass1.class);
            acceptedClasses.add(GoodClass2.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            lookAheadObjectInputStream.setEnabledSubclassing(true);
            lookAheadObjectInputStream.setEnabledMaxObjects(true);
            lookAheadObjectInputStream.setMaxObjects(2);
            int i = 0;
            while (i++ < 3) {
                lookAheadObjectInputStream.readObject();
            }
            fail("Deserialized more then specified max objects during testLimitingMaxObjects");
        } catch (SecurityException e) {
            //Good
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testLimitingMaxObjects");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testLimitingMaxObjects");
    }

    /**
     * Test that Primitive types (boolean, char, int,...), their wrappers (Boolean, Character, Integer,...) and String class
     * can always be deserialized
     */
    @Test
    public void testDeserializingPrimitiveTypes() throws Exception {
        log.trace(">testDeserializingPrimitiveTypes");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject((byte) 0);
            o.writeObject((short) 1);
            o.writeObject((int) 2);
            o.writeObject((long) 3);
            o.writeObject((float) 4);
            o.writeObject((double) 5);
            o.writeObject(new Byte((byte) 6));
            o.writeObject(new Short((short) 7));
            o.writeObject(new Integer((int) 8));
            o.writeObject(new Long((long) 9));
            o.writeObject(new Float((float) 10));
            o.writeObject(new Double((double) 11));
            o.writeObject(false);
            o.writeObject(new Boolean(true));
            o.writeObject('c');
            o.writeObject("String");
            o.writeObject(new byte[1]);
            o.writeObject(new short[1]);
            o.writeObject(new int[1]);
            o.writeObject(new long[1]);
            o.writeObject(new float[1]);
            o.writeObject(new double[1]);
            o.writeObject(new boolean[1]);

            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            lookAheadObjectInputStream.setEnabledMaxObjects(false);

            assertEquals("Data 0 corrupted during testDeserializingPrimitiveTypes", (byte) lookAheadObjectInputStream.readObject(), (byte) 0);
            assertEquals("Data 1 corrupted during testDeserializingPrimitiveTypes", (short) lookAheadObjectInputStream.readObject(), (short) 1);
            assertEquals("Data 2 corrupted during testDeserializingPrimitiveTypes", (int) lookAheadObjectInputStream.readObject(), (int) 2);
            assertEquals("Data 3 corrupted during testDeserializingPrimitiveTypes", (long) lookAheadObjectInputStream.readObject(), (long) 3);
            assertEquals("Data 4 corrupted during testDeserializingPrimitiveTypes", (float) lookAheadObjectInputStream.readObject(), (float) 4, 0);
            assertEquals("Data 5 corrupted during testDeserializingPrimitiveTypes", (double) lookAheadObjectInputStream.readObject(), (double) 5, 0);
            assertEquals("Data 6 corrupted during testDeserializingPrimitiveTypes", ((Byte) lookAheadObjectInputStream.readObject()).byteValue(), 6);
            assertEquals("Data 7 corrupted during testDeserializingPrimitiveTypes", ((Short) lookAheadObjectInputStream.readObject()).shortValue(),
                    7);
            assertEquals("Data 8 corrupted during testDeserializingPrimitiveTypes", ((Integer) lookAheadObjectInputStream.readObject()).intValue(),
                    8);
            assertEquals("Data 9 corrupted during testDeserializingPrimitiveTypes", ((Long) lookAheadObjectInputStream.readObject()).longValue(), 9);
            assertEquals("Data 10 corrupted during testDeserializingPrimitiveTypes", ((Float) lookAheadObjectInputStream.readObject()).floatValue(),
                    10, 0);
            assertEquals("Data 11 corrupted during testDeserializingPrimitiveTypes", ((Double) lookAheadObjectInputStream.readObject()).doubleValue(),
                    11, 0);
            assertEquals("Data 12 corrupted during testDeserializingPrimitiveTypes", (boolean) lookAheadObjectInputStream.readObject(), false);
            assertEquals("Data 13 corrupted during testDeserializingPrimitiveTypes",
                    ((Boolean) lookAheadObjectInputStream.readObject()).booleanValue(), true);
            assertEquals("Data 14 corrupted during testDeserializingPrimitiveTypes", (char) lookAheadObjectInputStream.readObject(), 'c');
            assertEquals("Data 15 corrupted during testDeserializingPrimitiveTypes",
                    ((String) lookAheadObjectInputStream.readObject()), "String");
            assertEquals("Data 16 corrupted during testDeserializingPrimitiveTypes", ((byte[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 17 corrupted during testDeserializingPrimitiveTypes", ((short[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 18 corrupted during testDeserializingPrimitiveTypes", ((int[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 19 corrupted during testDeserializingPrimitiveTypes", ((long[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 20 corrupted during testDeserializingPrimitiveTypes", ((float[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 21 corrupted during testDeserializingPrimitiveTypes", ((double[]) lookAheadObjectInputStream.readObject()).length, 1);
            assertEquals("Data 22 corrupted during testDeserializingPrimitiveTypes", ((boolean[]) lookAheadObjectInputStream.readObject()).length, 1);

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingPrimitiveTypes");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingPrimitiveTypes");
    }

    /**
     * Test deserializing subclass
     */
    @Test
    public void testDeserializingExtendedClasses() throws Exception {
        log.trace(">testDeserializingExtendedClasses");
        LookAheadObjectInputStream lookAheadObjectInputStream = null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(buf);
            o.writeObject(new GoodExtendedClass());
            o.writeObject(new GoodExtendedExtendedClass());
            o.close();
            lookAheadObjectInputStream = new LookAheadObjectInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Collection<Class<? extends Serializable>> acceptedClasses = new ArrayList<Class<? extends Serializable>>(3);
            acceptedClasses.add(GoodAbstractClass.class);
            lookAheadObjectInputStream.setAcceptedClasses(acceptedClasses);
            lookAheadObjectInputStream.setEnabledMaxObjects(false);
            lookAheadObjectInputStream.setEnabledSubclassing(true);
            @SuppressWarnings("unused")
            GoodExtendedClass goodExtendedClass = (GoodExtendedClass) lookAheadObjectInputStream.readObject();
            @SuppressWarnings("unused")
            GoodExtendedExtendedClass goodExtendedExtendedClass = (GoodExtendedExtendedClass) lookAheadObjectInputStream.readObject();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage() + " during testDeserializingExtendedClasses");
        } finally {
            if (lookAheadObjectInputStream != null) {
                lookAheadObjectInputStream.close();
            }
        }
        log.trace("<testDeserializingExtendedClasses");
    }

}