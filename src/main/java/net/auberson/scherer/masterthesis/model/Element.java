package net.auberson.scherer.masterthesis.model;

import org.apache.commons.csv.CSVRecord;

/**
 * Represents a Dataset element: Text and class.
 */
public class Element {

	final String text;
	final String classLabel;

	public Element(CSVRecord csvRecord) {
		this(csvRecord.get(0), csvRecord.get(1));
	}

	public Element(String text, String classLabel) {
		this.text = text;
		this.classLabel = classLabel;
	}

	public String getText() {
		return text;
	}

	public String getClassLabel() {
		return classLabel;
	}

	@Override
	public String toString() {
		return "text=" + truncate(text, 80) + " , classLabel=" + classLabel;
	}

	protected String truncate(String text, int length) {
		if (text == null) {
			return "";
		}
		if (text.length() <= length) {
			return text.trim();
		}
		return text.substring(0, length - 1).trim() + "... ";
	}

}
