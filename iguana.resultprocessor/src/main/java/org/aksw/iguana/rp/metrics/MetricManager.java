/**
 * 
 */
package org.aksw.iguana.rp.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * The MetricManager will manage all {@link org.aksw.iguana.rp.metrics.Metric}
 * 
 * @author f.conrads
 *
 */
public class MetricManager {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MetricManager.class);
	
	private Set<Metric> metrics = new HashSet<Metric>();

	private static MetricManager instance;

    public synchronized static MetricManager getInstance() {
		if (instance == null) {
			instance = new MetricManager();
		}
		return instance;
    }

    /**
	 * WIll add a metric to the manager
	 * @param metric
	 */
	public void addMetric(Metric metric){
		if(metric==null){
			return;
		}
		metrics.add(metric);
	}
	
	public Set<Metric> getMetrics(){
		return metrics;
	}
	
	/**
	 * Will add the meta Data to all metrics
	 * @param metaData
	 */
	public void addMetaData(Properties metaData){
		for(Metric m : metrics){
			m.setMetaData(metaData);
		}
	}
	/**
	 * This will message the received properties to all defined metrics.
	 * 
	 * @param p
	 */
	public void receiveData(Properties p){
		Set<Metric> remove = new HashSet<Metric>();
		for(Metric  m : metrics){
			try{
				m.receiveData(p);
			}catch(Exception e){
				LOGGER.warn("Could not use metric {}, Cause: {}",m.getShortName(),e);
				remove.add(m);
			}
		}
		metrics.removeAll(remove);
	}
	
	@Override 
	public String toString(){
		StringBuilder ret =new StringBuilder();
			
		Iterator<Metric> it = metrics.iterator();
		for(int i=0;i<metrics.size()-1;i++){
			
			ret.append(it.next().getShortName()).append(", ");
		}
		ret.append(it.next().getShortName());
		return ret.toString();
	}
	
	/**
	 * Will close all metrics
	 */
	public void close(){
		Set<Metric> remove = new HashSet<Metric>();
		for(Metric m : metrics){
			try{
				m.close();

			}catch(Exception e){
				LOGGER.error("Could not use metric "+m.getShortName()+".  Cause: {}",e);

			}
		}
		metrics.removeAll(remove);
	}

    public void addMetrics(List<Metric> metrics) {
		this.metrics.addAll(metrics);
    }
}
