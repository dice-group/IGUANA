package org.aksw.iguana.commons.rabbit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Easy to use RabbitMQ methods
 * 
 * @author f.conrads
 *
 */
public class RabbitMQUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQUtils.class);

	/**
	 * Getting an Object from a byte array
	 * 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getObject(byte[] data) {
		try (ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream is = new ObjectInputStream(in)) {
			return (T) is.readObject();
		} catch (IOException | ClassCastException | ClassNotFoundException e) {
			LOGGER.warn("Could not read receiving data", e);
		}
		return null;
	}

	/**
	 * Converting a Java Object to a byte array
	 * 
	 * @param t
	 * @return
	 */
	public static <T> byte[] getData(T t) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);) {
			
			oos.writeObject(t);
			oos.flush();
			oos.close();
			return bos.toByteArray();
		} catch (IOException e) {
			LOGGER.error("Could not convert Object " + t.getClass().getName() + " into byte array", e);
			return null;
		}
	}

}
