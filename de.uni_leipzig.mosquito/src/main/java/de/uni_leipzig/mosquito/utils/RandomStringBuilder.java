package de.uni_leipzig.mosquito.utils;

import java.util.Random;

/**
 * The Class RandomStringBuilder.
 * Generates a new random string
 * 
 * @author Felix Conrads
 */
public class RandomStringBuilder {

	
	/** The seed. */
	private long seed = 0L;
	
	/** The rand. */
	private Random rand;
	
	/** The alphanumeric. */
	private String alphanumeric = "abcdefghijklmnopqrstuvwxyz0123456789";
	
	/**
	 * Instantiates a new random string builder.
	 */
	public RandomStringBuilder(){
		rand = new Random(seed);
	}
	
	/**
	 * Instantiates a new random string builder.
	 *
	 * @param seed the seed
	 */
	public RandomStringBuilder(long seed){
		setSeed(seed);
	}
	
	/**
	 * Sets the seed.
	 *
	 * @param seed the new seed
	 */
	public void setSeed(long seed){
		this.seed =seed;
		rand = new Random(seed);
	}
	
	/**
	 * Builds the string.
	 *
	 * @param length the length the string should have
	 * @return the string
	 */
	public String buildString(int length){
		String randomString = "";
		for(int i=0; i<length; i++){
			int chr = rand.nextInt(alphanumeric.length()-1);
			randomString += alphanumeric.charAt(chr);
		}
		return randomString;
	}
	
}
