package org.aksw.iguana.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Class ZipUtils.
 * Saves a folder and it's files in a zip file
 */
public class ZipUtils {

	/**
	 * Writes a folder to a zipFile.
	 *
	 * @param srcFolder the src folder
	 * @param destFolder the zip file
	 * @return destFolder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String folderToZip(String srcFolder, String destFolder)
			throws IOException {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		fileWriter = new FileOutputStream(destFolder);
		zip = new ZipOutputStream(fileWriter);

		addFolderToZip("", srcFolder, zip);
		zip.flush();
		zip.close();
		return destFolder;
	}

	/**
	 * Adds a given folder to a zip file
	 *
	 * @param path the path in which the Folder should be saved in
	 * @param srcFolder the src folder
	 * @param zip the zipOutputStream to the zip file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void addFolderToZip(String path, String srcFolder,
			ZipOutputStream zip) throws IOException {
		File folder = new File(srcFolder);

		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/"
						+ fileName, zip);
			}
		}
	}

	/**
	 * Adds a given file to the zipFile.
	 *
	 * @param path the path in which the given file should be saved
	 * @param srcFile the src file
	 * @param zip the zip stream in which the file should be saved
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void addFileToZip(String path, String srcFile,
			ZipOutputStream zip) throws IOException {
		File src = new File(srcFile);
		if (src.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream(srcFile);
			zip.putNextEntry(new ZipEntry(path + "/" + src.getName()));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
			in.close();
		}
	}

}
