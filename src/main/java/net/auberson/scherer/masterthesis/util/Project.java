package net.auberson.scherer.masterthesis.util;

import java.io.File;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;

/**
 * Bits and pieces used throughout the Project, such as directory paths
 */
public class Project {
	// Directories
	public static final File STACKEXCHANGE_RAW_DATA_DIR = new File("./data/raw/stackexchange");
	public static final File INTERMEDIATE_DATA_DIR = new File("./data/intermediate");
	public static final File PROCESSED_DATA_DIR = new File("./data/processed");
	public static final File LEARNING_CURVE_REPORTS_DIR = new File("./reports/learning-curve");
	
	// Files
	public static final File getDataFile(String name) {
		return new File(INTERMEDIATE_DATA_DIR, name + ".csv");
	}
	public static final File DATAFILE_COUNT = getDataFile("count");
	
	// NLP Classifier input format: Defines the characters allowed for NLC input
	public static final CharMatcher NLC_FORMAT = CharMatcher.javaLetterOrDigit().or(CharMatcher.anyOf("!?:;.'/()& "))
			.precomputed();

	// Characters that match the end of a sentence. Used to shorten NLC input
	public static final CharMatcher NLC_SENTENCE_END = CharMatcher.anyOf("!?:;.").precomputed();

	// Stop words in site names: Ignore meta boards, and boards in language the NLC
	// doesn't support.
	public static final String[] ignoredBoards = new String[] { "meta.", "arabic" };

	// A Regexp pattern that matches HTML tags. Taken from
	// https://stackoverflow.com/questions/3607965/how-to-convert-html-text-to-plain-text
	public static final Pattern HTML_TAGS_REGEXP = Pattern.compile("(?s)<[^>]*>(\\s*<[^>]*>)*");
	
	// IBM Cloud NLC 
	public static final int MAX_SAMPLES_PER_TRAINING = 20000;
	public static final int MAX_SAMPLES_PER_CLASSIFICATION_REQUEST = 30;
}
