package org.aksw.iguana.cc.worker;

/**
 * The Strategy Names to simulate different network latency behaviors
 * 
 * @author f.conrads
 *
 */
public enum LatencyStrategy {
    /**
     * No Latency should be simulated
     */
    NONE, 
    
    /**
     * A fixed time/ms should be waited between queries (time is the latency base value)
     */
    FIXED, 
    
    /**
     * The time/ms should be calculated randomly each time 
     * out of a gaussian intervall based on the latency base value as follows
     * 
     * [0;2*latencyBaseValue]
     */
    VARIABLE
}
