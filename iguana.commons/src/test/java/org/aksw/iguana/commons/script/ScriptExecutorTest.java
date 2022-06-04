package org.aksw.iguana.commons.script;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ScriptExecutorTest {

    private static Logger LOGGER = LoggerFactory.getLogger(ScriptExecutorTest.class);

    private String cmd;
    private String[] args;
    private int expectedExitCode;
    private Method callbackMethod;
    private Object[] callbackArgs=new Object[]{};

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        //simple method
        if (SystemUtils.IS_OS_LINUX) {
            testConfigs.add(new Object[]{"/bin/touch", new String[]{"ShouldNotExistWhatSoEver"}, 0,
                    "removeFile", new Object[]{"ShouldNotExistWhatSoEver"}});
        } else if (SystemUtils.IS_OS_MAC) {
            testConfigs.add(new Object[]{"/usr/bin/touch", new String[]{"ShouldNotExistWhatSoEver"}, 0,
                    "removeFile", new Object[]{"ShouldNotExistWhatSoEver"}});
        }
        //testing if additional arguments are checked
        testConfigs.add(new Object[]{"/bin/echo test", new String[]{"123", "456"}, 0, "emptyCallback", new Object[]{}});
        //should fail as file not exist
        testConfigs.add(new Object[]{"scriptThatShouldNotExist", new String[]{}, -1, "emptyCallback", new Object[]{}});
        //should fail with 1

        return testConfigs;
    }



    public ScriptExecutorTest(String cmd, String[] args, int expectedExitCode, String callbackMethodName, Object[] callbackArgs) throws NoSuchMethodException {
        this.cmd=cmd;
        this.args=args;
        this.expectedExitCode=expectedExitCode;
        this.callbackArgs = callbackArgs;
        Class<?>[] classes = new Class<?>[callbackArgs.length];
        for(int i=0;i<callbackArgs.length;i++){
            Object arg = callbackArgs[i];
            classes[i] = arg.getClass();
        }
        callbackMethod = this.getClass().getDeclaredMethod(callbackMethodName, classes);
    }

    @Test
    public void checkExecution() throws IOException, InvocationTargetException, IllegalAccessException {
        if(expectedExitCode!=0){
            LOGGER.warn("Exception should be thrown in this test.");
        }
        int exitCode = ScriptExecutor.execSafe(cmd, args);
        assertEquals(expectedExitCode, exitCode);
        callbackMethod.invoke(this, callbackArgs);
    }

    private void emptyCallback(){}

    private void removeFile(String fileName){
        File f = new File(fileName);
        assertTrue(f.exists());
        f.delete();
    }

}
