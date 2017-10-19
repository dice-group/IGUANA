package org.aksw.iguana.cc.config;

import org.aksw.iguana.commons.config.ConfigurationUtils;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.apache.commons.configuration.ConfigurationException;

/**
 * This class will load a Configuration and send it to the correct RabbitMQ Queue
 * @author f.conrads
 *
 */
public class ConfigSender {
	
	/**
	 * Will send the configuration (second argument) to the RABBITMQ Queue (config2mc) on the rabbitmq host (first argument)
	 * 
	 * @param args
	 * @throws ConfigurationException
	 */
	public static void main(String[] args) throws ConfigurationException {
		if(args.length!=2) {
			System.out.println("Usage: java -cp \"target/lib/*\" "+ConfigSender.class.getName()+" rabbitmq-host config-file\n");
			System.out.println("\trabbitmq-host: the host the rabbitmq server runs on (e.g. localhost)");
			System.out.println("\tconfig-file: the Iguana Config (see org.apache.commons.configuration.Configuration for more details)");
		}
		else {
			ISender sender = new DefaultSender();
			sender.init(args[0], COMMON.CONFIG2MC_QUEUE_NAME);
			sender.send(RabbitMQUtils.getData(ConfigurationUtils.convertConfiguration(args[1])));
			System.out.println("Finished");
			sender.close();
		}
	}

}
