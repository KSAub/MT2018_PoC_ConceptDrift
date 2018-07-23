package net.auberson.scherer.masterthesis.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

/**
 * Utility class for simple I/O. <br>
 * In general, will abort program execution on Exceptions.
 */
public class IOUtil {

	private IOUtil() {
		// Can't instantiate this!
	}

	/**
	 * Opens a CSV file for parsing
	 */
	public static CSVParser openCSV(File file) {
		try {
			return CSVFormat.DEFAULT.parse(new FileReader(file));
		} catch (IOException e) {
			System.err.println("Unable to parse the CSV file at '" + file.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	/**
	 * Opens a file for output
	 */
	public static PrintWriter getWriter(File file) {
		try {
			return new PrintWriter(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file for output at '" + file.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	
	/**
	 * Opens a file for output, appending to an existing file instead of overwriting
	 */
	public static PrintWriter getAppendingWriter(File file) {
		try {
			return new PrintWriter(new FileWriter(file, true));
		} catch (IOException e) {
			System.err.println("Unable to open file for output at '" + file.getAbsolutePath() + "'");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	
	/**
	 * Close any kind of IO resource
	 */
	public static void close(Closeable o) {
		try {
			o.close();
		} catch (IOException e) {
			System.err.println("Unable to close the file.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
