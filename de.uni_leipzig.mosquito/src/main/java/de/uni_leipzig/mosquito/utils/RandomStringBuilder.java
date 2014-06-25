package de.uni_leipzig.mosquito.utils;

import java.util.Random;

public class RandomStringBuilder {

	public static void main(String args[]){
		RandomStringBuilder rsb1 = new RandomStringBuilder();
		RandomStringBuilder rsb2 = new RandomStringBuilder();
		for(int i=0; i<100; i++){
			System.out.println(rsb1.buildString(10));
			System.out.println(rsb2.buildString(10));
			System.out.println(rsb1.buildString(20));
			System.out.println(rsb2.buildString(20));
		}
	}
	
	private long seed = 0L;
	private Random rand;
	private String alphanumeric = "abcdefghijklmnopqrstuvwxyz0123456789";
	
	public RandomStringBuilder(){
		rand = new Random(seed);
	}
	public RandomStringBuilder(long seed){
		setSeed(seed);
	}
	
	public void setSeed(long seed){
		this.seed =seed;
		rand = new Random(seed);
	}
	
	public String buildString(int length){
		String randomString = "";
		for(int i=0; i<length; i++){
			int chr = rand.nextInt(alphanumeric.length()-1);
			randomString += alphanumeric.charAt(chr);
		}
		return randomString;
	}
	
}
