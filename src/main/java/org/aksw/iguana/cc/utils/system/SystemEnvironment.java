package org.aksw.iguana.cc.utils.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.io.StringWriter;

public record SystemEnvironment(
        String cpuName,
        long totalRam,
        String osFamily,
        String osVersion,
        String javaRuntimeName,
        String javaRuntimeVersion
) {

    public static SystemEnvironment getSystemEnvironment() {
        SystemInfo si = new SystemInfo(); // or new SystemInfoFFM() on java25 version
        HardwareAbstractionLayer hal = si.getHardware();
        final var cpu = hal.getProcessor();
        final var memory = hal.getMemory();

        final var cpuName = cpu.getProcessorIdentifier().getName();
        final var totalRam = memory.getTotal();
        final var osFamily = si.getOperatingSystem().getFamily();
        final var osVersion = si.getOperatingSystem().getVersionInfo().getVersion();
        final var javaRuntimeName = System.getProperty("java.runtime.name");
        final var javaRuntimeVersion = System.getProperty("java.runtime.version");

        return new SystemEnvironment(
                cpuName,
                totalRam,
                osFamily,
                osVersion,
                javaRuntimeName,
                javaRuntimeVersion
        );
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        String out = "";
        try (StringWriter sw = new StringWriter()) {
            mapper.writeValue(sw, this);
            out = sw.toString();
        } catch (IOException e) {
            // unlikely
            throw new RuntimeException(e);
        }
        return out;
    }
}
