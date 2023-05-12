package org.aksw.iguana.commons.number;


import org.aksw.iguana.commons.numbers.NumberUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class NumberUtilsTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        //simple method
        testConfigs.add(new Object[]{"123", Long.class, 123L});
        testConfigs.add(new Object[]{"123.0", Double.class, 123.0});
        testConfigs.add(new Object[]{"123", Double.class, 123.0});
        testConfigs.add(new Object[]{"123.A", Double.class, null});
        testConfigs.add(new Object[]{"123.A", Long.class, null});
        testConfigs.add(new Object[]{"123.0123", Double.class, 123.0123});
        testConfigs.add(new Object[]{null, Double.class, null});
        testConfigs.add(new Object[]{null, Long.class, null});

        return testConfigs;
    }

    private String number;
    private Class<? extends Number> clazz;
    private Number expected;

    public NumberUtilsTest(String number, Class<? extends Number> clazz, Number expected){
        this.number=number;
        this.expected = expected;
        this.clazz=clazz;
    }

    @Test
    public void checkForClass(){
        if(clazz == Long.class){
            assertEquals(expected, NumberUtils.getLong(number));
        }
        else if(clazz == Double.class) {
            assertEquals(expected, NumberUtils.getDouble(number));

        }
    }

}
