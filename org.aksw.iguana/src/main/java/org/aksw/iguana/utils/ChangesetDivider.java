package org.aksw.iguana.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ChangesetDivider {

	public static void main(String[] argc) throws IOException {
		long maxFiles = Long.valueOf(argc[2]);
		long limit = Math.max(FileHandler.getLineCount(argc[0])
				,FileHandler.getLineCount(argc[1]))/maxFiles;
		divide(argc[0], true, limit);
		divide(argc[1], false, limit);
	}

	public static void divide(String file, boolean add, long limit) throws IOException {
		divide(new File(file), add, limit);
	}

	public static void divide(File file, boolean add, long limit)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		String name = "";
		if (add) {
			name = ".added.nt";
		} else {
			name = ".removed.nt";
		}
		int count = 0;
		long triple = 0;
		PrintWriter pw = new PrintWriter(String.format("%06d", count) + name);
		String line;
		while ((line = br.readLine()) != null) {
			pw.println(line);
			triple++;
			if (triple >= limit) {
				count++;
				pw.close();
				pw = new PrintWriter(String.format("%06d", count) + name);
				triple=0;
			}
		}
		pw.close();
		br.close();

	}
}
