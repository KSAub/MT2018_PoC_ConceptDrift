package net.auberson.scherer.masterthesis.model;

import java.util.Comparator;

import org.apache.commons.csv.CSVRecord;

public class Result {

	final String text;
	final String expectedClass;
	final String detectedClass;
	final Double confidence;

	public Result(CSVRecord csvRecord) {
		this(csvRecord.get(0), csvRecord.get(1), csvRecord.get(2), Double.parseDouble(csvRecord.get(3)));
	}

	public Result(String text, String expectedClass, String detectedClass, Double confidence) {
		this.text = text;
		this.expectedClass = expectedClass;
		this.detectedClass = detectedClass;
		this.confidence = confidence;
	}

	@Override
	public String toString() {
		return "text=" + text.substring(0, Math.min(80, text.length() - 1)) + "... , expectedClass=" + expectedClass
				+ ", detectedClass=" + detectedClass + ", confidence=" + confidence;
	}

	public static final Comparator<Result> COMPARATOR = new Comparator<Result>() {
		public int compare(Result o1, Result o2) {
			return Double.compare(o1.confidence, o2.confidence);
		}
	};

}
