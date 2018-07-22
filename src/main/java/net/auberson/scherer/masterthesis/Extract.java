package net.auberson.scherer.masterthesis;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Date;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.CharMatcher;
import com.google.gson.internal.bind.util.ISO8601Utils;

import net.auberson.scherer.masterthesis.util.Project;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

/**
 * 'Extract' executable: Extracts the dataset. <br>
 * Expects the unpacked StackOverflow archive in ./data/raw (i.e. a subdirectory
 * named 'stackoverflow' containing a number of 7z files). Generates many
 * dataset CSVs in ./data/intermediate (one CSV per class, and a CSV containing
 * the dataset sizes).
 */
public class Extract {
	// private static final DocumentBuilderFactory dbFactory =
	// DocumentBuilderFactory.newInstance();
	private static final SAXParserFactory factory = SAXParserFactory.newInstance();
	private static long datasetCount;
	private static long datasetCountTotal;

	/**
	 * Executable Java Program. Processes all files in data/raw into files in
	 * data/intermediate
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Project.DATAFILE_COUNT.delete();
		PrintWriter countOut = new PrintWriter(Project.DATAFILE_COUNT);

		System.out.println("Extracting data set from archive...");

		for (File file : Project.STACKEXCHANGE_RAW_DATA_DIR.listFiles()) {
			if (file.getName().endsWith("7z")) {
				extractArchiveContents(file, countOut);
			}
		}

		countOut.close();
		System.out.println("Extracted data set from archive, " + datasetCountTotal + " elements written in total");
	}

	/**
	 * Processes a single 7zip file: If the 7zip file contains a file called
	 * "Posts.xml", that file is passed to parsePostsXml.
	 * 
	 * @param file
	 *            a 7zip file
	 * @param countOut
	 * @throws Exception
	 */
	private static void extractArchiveContents(File file, PrintWriter countOut) throws Exception {
		String siteName = file.getName().replaceAll(".stackexchange|.com|.7z", "");
		for (String ignoredBoard : Project.ignoredBoards) {
			if (siteName.contains(ignoredBoard)) {
				// Abort processing if site name contains a board stopword (e.g. meta)
				return;
			}
		}

		RandomAccessFileInStream inputStream = new RandomAccessFileInStream(new RandomAccessFile(file, "r"));
		ISimpleInArchive archive = SevenZip.openInArchive(null, inputStream).getSimpleInterface();
		int fileCount = archive.getNumberOfItems();
		for (int i = 0; i < fileCount; i++) {
			ISimpleInArchiveItem archiveItem = archive.getArchiveItem(i);
			if (archiveItem.getPath().equals("Posts.xml")) {
				parsePostsXml(archiveItem, siteName, countOut);
			}
		}
	}

	/**
	 * Processes a single Posts.xml entry in a 7zip file: Extract it to a temporary
	 * location, and parse the XML contents. Pass the results to processPosts.
	 * 
	 * @param archiveItem
	 *            a 7zip entry in a 7zip file
	 * @param siteName
	 *            the name of the original 7zip file, without the extension (e.g.
	 *            cooking.stackexchange.com)
	 * @param countOut
	 *            the file to which to write the number of samples extracted once a
	 *            dataset file has been created
	 * @throws Exception
	 */
	private static void parsePostsXml(ISimpleInArchiveItem archiveItem, final String siteName, PrintWriter countOut)
			throws Exception {
		long archiveSize = archiveItem.getSize().longValue();
		if (archiveSize <= 0) {
			System.out.println("Warning: Archive size is " + archiveSize + ": " + siteName);
			return;
		}

		// Create a temporary file. This is created on this computer's default temp
		// directory automatically
		File tempFile = File.createTempFile("temp", ".xml");

		// Extract posts.xml to the temporary file
		RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");
		ExtractOperationResult result = archiveItem.extractSlow(new RandomAccessFileOutStream(randomAccessFile));
		randomAccessFile.close();
		if (!result.equals(ExtractOperationResult.OK)) {
			System.err.println("Error: 7z Extract result is " + result.name());
			return;
		}

		// Create a CSV file where the filtered content will be output
		File datasetFile = Project.getDataFile(siteName);
		datasetFile.delete();
		final PrintWriter datasetOut = new PrintWriter(datasetFile);

		// Parse the temporary file
		factory.newSAXParser().parse(tempFile, new DefaultHandler() {
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes)
					throws SAXException {
				processXmlRow(qName, attributes, siteName, datasetOut);
			}
		});

		// Delete the temporary file
		tempFile.delete();

		// Write out the number of samples, close the output file
		countOut.println(siteName + ", " + datasetCount + ", " + ISO8601Utils.format(new Date()));
		countOut.flush();
		datasetCountTotal = datasetCountTotal + datasetCount;
		datasetCount = 0;
		datasetOut.close();

		System.out.println("Processed " + siteName);
	}

	/**
	 * Processes a single XML element: Check whether it's a "row" element, whether
	 * that row is a Question, and pass the title and body attributes to processPost
	 * if it is.
	 * 
	 * @param tagName
	 *            the name of the XML tag
	 * @param attributes
	 *            the XML attributes in this tag
	 * @param siteName
	 *            the name of the original 7zip file, without the extension (e.g.
	 *            cooking.stackexchange.com)
	 * @param datasetOut
	 *            the output file to which to write dataset items
	 */
	private static void processXmlRow(String tagName, Attributes attributes, String siteName, PrintWriter datasetOut) {
		if (tagName.equalsIgnoreCase("row")) {
			String type = attributes.getValue("PostTypeId");
			// See the list of post types at:
			// https://meta.stackexchange.com/questions/99265/meaning-of-values-for-posttypeid-in-data-explorer-or-in-data-dump
			if (Integer.parseInt(type) == 1) {
				// Only process Questions, type 1. Ignore answers, tags, etc...
				String title = attributes.getValue("Title");
				String body = attributes.getValue("Body");
				processPost(title, body, siteName, datasetOut);
			}
		}
	}

	/**
	 * Processes a single post: adds the contents to our data set as a single,
	 * filtered line, with the board name as a class.
	 * 
	 * @param title
	 *            The post's title in HTML
	 * @param body
	 *            The post's body in HTML
	 * @param siteName
	 *            the name of the original 7zip file, without the extension (e.g.
	 *            cooking.stackexchange.com)
	 * @param datasetOut
	 *            the output file to which to write dataset items
	 */
	private static void processPost(String title, String body, String siteName, PrintWriter datasetOut) {
		String text = cleanupInput(title) + " \\r " + cleanupInput(body);

		if (text.length() > 1024) {
			int cutAt = -1;

			// Find the next end-of-sentence punctuation mark, until it's past 1023 chars.
			int dotPosition = Project.NLC_SENTENCE_END.indexIn(text);
			if (dotPosition != -1) {
				while (dotPosition < 1023 && dotPosition != -1) {
					cutAt = dotPosition;
					dotPosition = Project.NLC_SENTENCE_END.indexIn(text, cutAt + 1);
				}
			} else {
				// No end-of-sentence mark found? Search for whitespace instead.
				int spacePosition = CharMatcher.whitespace().indexIn(text);
				while (spacePosition < 1023 && spacePosition != -1) {
					cutAt = spacePosition;
					spacePosition = CharMatcher.whitespace().indexIn(text, cutAt + 1);
				}
			}

			// No whitespace found either? Cut at 1024 chars.
			if (cutAt == -1) {
				cutAt = 1023;
			}

			text = text.substring(0, cutAt + 1);
		}

		// Save the result to our output file.
		datasetOut.println("\"" + text + "\", " + siteName);
		datasetCount++;
	}

	/**
	 * Clean up a line of text as expected by the Natural Language Classifier
	 * 
	 * @param text
	 * @return
	 */
	private static String cleanupInput(String text) {
		// First, remove HTML tags, and replace them by whitespace
		text = Project.HTML_TAGS_REGEXP.matcher(text).replaceAll(" ");
		// Then, remove HTML entities (e.g. '&amp;' becomes '&')
		text = StringEscapeUtils.unescapeHtml4(text);
		// Then, replace any whitespace by a standard whitespace
		// https://github.com/google/guava/wiki/StringsExplained
		text = CharMatcher.whitespace().trimAndCollapseFrom(text, ' ');
		// Then, remove the remaining special characters
		text = Project.NLC_FORMAT.retainFrom(text);

		return text;
	}
}
