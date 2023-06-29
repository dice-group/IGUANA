package org.aksw.iguana.commons.script;

import org.aksw.iguana.commons.utils.ServerMock;
import org.junit.Test;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.Assert.assertEquals;

public class ScriptExecutorWaitTest {
    private static final int FAST_SERVER_PORT = 8023;
    private ServerMock fastServerContainer;
    private ContainerServer fastServer;
    private SocketConnection fastConnection;

    @Test
    public void issue108() throws IOException {
        fastServerContainer = new ServerMock();
        fastServer = new ContainerServer(fastServerContainer);
        fastConnection = new SocketConnection(fastServer);
        SocketAddress address1 = new InetSocketAddress(FAST_SERVER_PORT);
        fastConnection.connect(address1);

        File f = new File("shouldNotExist.pid");
        f.createNewFile();
        int exitCode = ScriptExecutor.execSafe("src/test/resources/complex-script-example-issue108.sh", new String[]{});
        assertEquals(1, exitCode);
        f.delete();
        exitCode = ScriptExecutor.execSafe("src/test/resources/complex-script-example-issue108.sh", new String[]{});
        assertEquals(0, exitCode);
        fastConnection.close();
        fastServer.stop();
    }
}
