package org.aksw.iguana.cc.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.*;

@Disabled("CLI doesn't work right now")
public class CLIProcessManagerTest {

    @Test
    public void execTest() throws InterruptedException {
        //create process
        Process p = CLIProcessManager.createProcess("echo \"abc\"; wait 1m");
        //destroy process
        assertTrue(p.isAlive());
        CLIProcessManager.destroyProcess(p);
        //give OS a little bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

    @Test
    public void countLinesSuccessfulTest() throws IOException, InterruptedException {
        //create
        Process p = CLIProcessManager.createProcess("echo \"abc\"; wait 100; echo \"t\nt\nabc: test ended suffix\"; wait 1m;");
        //count Lines until "test ended" occured
        Thread.sleep(100);
        assertTrue(CLIProcessManager.isReaderReady(p));

        assertEquals(3, CLIProcessManager.countLinesUntilStringOccurs(p, "test ended", "failed"));
        //destroy
        CLIProcessManager.destroyProcess(p);
        //give OS a little bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

    @Test
    public void countLinesFailTest() throws IOException, InterruptedException {
        //create
        Process p = CLIProcessManager.createProcess("echo \"abc\"; wait 100; echo \"abc: failed suffix\"; wait 1m;");
        Thread.sleep(100);
        assertTrue(CLIProcessManager.isReaderReady(p));
        //count Lines until "test ended" occured
        try{
            CLIProcessManager.countLinesUntilStringOccurs(p, "test ended", "failed");
            assertTrue("Test did not end in IOException", false);
        }catch (IOException e){
            assertTrue(true);
        }
        //destroy
        CLIProcessManager.destroyProcess(p);
        //give OS a little bit of time to destroy process
        Thread.sleep(50);
        assertFalse(p.isAlive());

    }

}
