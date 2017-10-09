package org.aksw.iguana.wc.exec;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.primefaces.event.FlowEvent;

/**
 * Controller to add a Config convert the Configuration to a Properties Object and send it to 
 * the Core Controller. Thus starting an Iguana Suite
 * 
 * @author f.conrads
 *
 */
@Named
@SessionScoped
public class ExecutionController implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5112905497758755682L;

	
    public String onFlowProcess(FlowEvent event) {
       return event.getNewStep();
        
    }
}
