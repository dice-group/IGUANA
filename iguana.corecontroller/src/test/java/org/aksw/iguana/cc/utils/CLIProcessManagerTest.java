package org.aksw.iguana.cc.utils;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class CLIProcessManagerTest {

    @Test
    public void execTest() throws InterruptedException {
        //create process
        Process p = CLIProcessManager.createProcess("/usr/bin/printf \"abc\"");
        //destroy process
        assertTrue(p.isAlive());
        CLIProcessManager.destroyProcess(p);
        //give OS a bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

    @Test
    public void countLinesSuccessfulTest() throws IOException, InterruptedException {
        //create
        String cmd = "echo ";
        if (SystemUtils.IS_OS_MAC) {
            cmd = "/usr/bin/printf ";   // More consistent
        }
        Process p = CLIProcessManager.createProcess(cmd + " \"abc\"; " + cmd + " \"t\\nt\\nabc: test ended suffix\";");
        //count Lines until "test ended" occurs
        Thread.sleep(100);
        assertTrue(CLIProcessManager.isReaderReady(p));

        assertEquals(3, CLIProcessManager.countLinesUntilStringOccurs(p, "test ended", "failed"));
        //destroy
        CLIProcessManager.destroyProcess(p);
        //give OS a bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

    @Test
    public void countLinesFailTest() throws IOException, InterruptedException {
        //create
        String cmd = "echo ";
        if (SystemUtils.IS_OS_MAC) {
            cmd = "/usr/bin/printf ";   // More consistent
        }
        Process p = CLIProcessManager.createProcess(cmd + " \"abc\"; " + cmd + " \"abc: failed suffix\";");
        Thread.sleep(100);
        assertTrue(CLIProcessManager.isReaderReady(p));
        //count Lines until "test ended" occurs
        try{
            CLIProcessManager.countLinesUntilStringOccurs(p, "test ended", "failed");
            assertTrue("Test did not end in IOException", false);
        }catch (IOException e){
            assertTrue(true);
        }
        //destroy
        CLIProcessManager.destroyProcess(p);
        //give OS a bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

}
