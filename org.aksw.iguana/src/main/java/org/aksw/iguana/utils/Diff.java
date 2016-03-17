package org.aksw.iguana.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.UUID;

import org.aksw.iguana.utils.comparator.TripleComparator;

public class Diff {

	public static void main(String[] argc) throws IOException{
		if(argc.length<2){
			System.out.println("Usage: java -cp \"lib/*\" "+Diff.class.getCanonicalName()+" dump1.nt dump2.nt");
			return;
		}
		diffToFiles(argc[0], argc[1], "only_in_first.nt",
				"only_in_second.nt",
				"in_both.nt");
	}
	
	public static void diffToFiles(String input_pre, String input_cur, 
			String output_in_pre, String output_in_cur, String output_in_both) throws IOException{
		//Sort inputs
		String sorted1Name = UUID.randomUUID()+".nt", sorted2Name= UUID.randomUUID()+".nt";
		Comparator<String> cmp = new TripleComparator();
		
		File sorted1 = new File(sorted1Name);
		sorted1.createNewFile();
		ExternalSort.mergeSortedFiles( 
				ExternalSort.sortInBatch(new File(input_pre), cmp, false), sorted1, cmp);
		
		File sorted2 = new File(sorted2Name);
		ExternalSort.mergeSortedFiles(
				ExternalSort.sortInBatch(new File(input_cur), cmp, false), sorted2, cmp);
		
		BufferedReader br1 = new BufferedReader(new FileReader(sorted1));
		BufferedReader br2 = new BufferedReader(new FileReader(sorted2));
		
		PrintWriter pw1=new PrintWriter(output_in_pre) , 
				pw2= new PrintWriter(output_in_cur), 
				pw3=new PrintWriter(output_in_both);
		
		diffToFiles(br1, br2, pw1, pw2, pw3);
		br1.close();
		br2.close();
		pw1.close();
		pw2.close();
		pw3.close();
		sorted1.deleteOnExit();
		sorted2.deleteOnExit();
	}
	
	public static void diffToFiles(BufferedReader input_pre, BufferedReader input_cur, 
			PrintWriter output_in_pre, PrintWriter output_in_cur, 
			PrintWriter output_in_both) throws IOException{
		//Remember, files are sorted
		String curLine1="", curLine2="";
		//Read both=0, 1=1, 2=2
		byte b = 0;
		TripleComparator cmp = new TripleComparator();
		while(curLine1!=null||curLine2!=null){
			//check if same, 1 is > or 2 is >
			if(b==0){
				curLine1 = input_pre.readLine();
				curLine2 = input_cur.readLine();
			}
			if(b==1){
				curLine1 = input_pre.readLine();
			}
			if(b==2){
				curLine2 = input_cur.readLine();
			}
			if(curLine1==null){
				if(curLine2==null){
					break;
				}
				output_in_pre.println(curLine2);
				b=2;
				continue;
			}
			if(curLine2==null){
				output_in_cur.println(curLine1);
				b=1;
				continue;
			}
			if(cmp.compare(curLine1, curLine2)==0){
				output_in_both.println(curLine1);
				b=0;
			}
			//Read & Write until same or greater
			else if(cmp.compare(curLine1, curLine2)>0){
				//curLine1  > curLine2
				//Writer curLine2 and read CurLine2 only next time
				output_in_cur.println(curLine2);
				b=2;
			}
			else if(cmp.compare(curLine1, curLine2)<0){
				//curLine2 > curLine1
				
				output_in_pre.println(curLine1);
				b=1;
			}

		}
		
	}
	
}
