package org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.update.UpdateTimer.Strategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This will test if the {@link UpdateTimer} method to check if the time to wait
 * between two updates is calculated correctly
 * @author f.conrads
 *
 */
@RunWith(Parameterized.class)
public class UpdateTimerTest {


	/**
	 * @return Configurations to test
	 */
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();
		testConfigs.add(new Object[] { "NONE", 10000, 6000, 1000, 60000 });
		testConfigs.add(new Object[] { "FIXED", 320, 6000, 1000, 60000 });
		testConfigs.add(new Object[] { "DISTRIBUTED", 10000, 6000, 1000, 60000 });
		return testConfigs;
	}
	
	private Strategy strategy;
	private Long baseValue;
	private long timeExceeded;
	private long executedQueries;
	private long timeLimit;


	public UpdateTimerTest(String strategy, long baseValue, long timeExceeded, long executedQueries, long timeLimit) {
		this.strategy = UpdateTimer.Strategy.valueOf(strategy);
		this.baseValue = baseValue;
		this.timeExceeded = timeExceeded;
		this.executedQueries = executedQueries;
		this.timeLimit = timeLimit;
	}

	/**
	 * Tests if the calculated time between two updates is correct
	 */
	@Test
	public void testTime() throws IOException {
		switch(strategy) {
		case NONE:
			UpdateTimer timer = new UpdateTimer();
			assertEquals(0, timer.calculateTime(timeExceeded, executedQueries));
			break;
		case FIXED:
			timer = new UpdateTimer(baseValue);
			assertEquals(baseValue.longValue(), timer.calculateTime(timeExceeded, executedQueries));
			break;
		case DISTRIBUTED:
			timer = new UpdateTimer(baseValue.intValue(), timeLimit);
			long expected = (timeLimit-timeExceeded)/(baseValue-executedQueries);
			assertEquals(expected, timer.calculateTime(timeExceeded, executedQueries));
		default:			
		}
	}

}
