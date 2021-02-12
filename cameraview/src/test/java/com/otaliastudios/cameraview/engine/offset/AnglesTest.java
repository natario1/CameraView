package com.otaliastudios.cameraview.engine.offset;


import com.otaliastudios.cameraview.controls.Facing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnglesTest {

    private Angles angles;

    @Before
    public void setUp() {
        angles = new Angles();
    }

    @After
    public void tearDown() {
        angles = null;
    }

    @Test
    public void testSetSensorOffset() {
        angles.setSensorOffset(Facing.BACK, 90);
        assertEquals(90, angles.mSensorOffset);
        angles.setSensorOffset(Facing.FRONT, 90);
        assertEquals(270, angles.mSensorOffset);
    }

    @Test
    public void testSetDisplayOffset() {
        angles.setDisplayOffset(90);
        assertEquals(90, angles.mDisplayOffset);
    }

    @Test
    public void testSetDeviceOrientation() {
        angles.setDeviceOrientation(90);
        assertEquals(90, angles.mDeviceOrientation);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetSensorOffset_throws() {
        angles.setSensorOffset(Facing.BACK, 135);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetDisplayOffset_throws() {
        angles.setDisplayOffset(135);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetDeviceOrientation_throws() {
        angles.setDeviceOrientation(135);
    }

    @Test
    public void testOffset_BaseToSensor() {
        angles.setSensorOffset(Facing.BACK, 90);
        assertEquals(270, angles.offset(Reference.BASE, Reference.SENSOR, Axis.RELATIVE_TO_SENSOR));
        angles.setSensorOffset(Facing.FRONT, 270); // This is like setting 90
        assertEquals(90, angles.offset(Reference.BASE, Reference.SENSOR, Axis.RELATIVE_TO_SENSOR));
    }

    @Test
    public void testOffset_BaseToView() {
        angles.setDisplayOffset(90);
        assertEquals(270, angles.offset(Reference.BASE, Reference.VIEW, Axis.ABSOLUTE));
    }

    @Test
    public void testOffset_BaseToOutput() {
        angles.setDeviceOrientation(90);
        assertEquals(90, angles.offset(Reference.BASE, Reference.OUTPUT, Axis.ABSOLUTE));
    }
}
