package net.auberson.scherer.masterthesis.model;

import java.io.PrintWriter;
import java.util.List;

public class StatisticsResults {

	// True Positive
	final int tp;
	// False Positive
	final int fp;
	// False Negative
	final int fn;
	// True Negative
	final int tn;

	// Recall
	final double tpr;
	// Precision
	final double ppv;
	// True Negative Rate
	final double tnr;
	// Informedness
	final double bm;
	// Accuracy
	final double acc;
	// Error Rate
	final double err;
	// F1 Score
	final double f1;

	public StatisticsResults(StatisticsCounter source) {
		this(source.tp, source.fp, source.fn, source.tn);
	}

	public StatisticsResults(int tp, int fp, int fn, int tn) {
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.tn = tn;

		double TP = tp;
		double FP = fp;
		double FN = fn;
		double TN = tn;

		this.tpr = TP / (TP + FN);
		this.ppv = TP / (TP + FP);
		this.tnr = TN / (TN + FP);
		this.bm = tpr + tnr - 1;
		this.acc = (TP + TN) / (TP + TN + FP + FN);
		this.err = (FP + FN) / (TP + TN + FP + FN);
		this.f1 = 2 * ((ppv * tpr) / (ppv + tpr));
	}

	public static StatisticsResults aggregate(List<StatisticsCounter> counters) {
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;

		for (StatisticsCounter counter : counters) {
			tp += counter.tp;
			fp += counter.fp;
			fn += counter.fn;
			tn += counter.tn;
		}

		return new StatisticsResults(tp, fp, fn, tn);
	}

	@Override
	public String toString() {
		return "StatisticsResults [tp=" + tp + ", fp=" + fp + ", fn=" + fn + ", tn=" + tn + ", tpr=" + tpr + ", ppv="
				+ ppv + ", tnr=" + tnr + ", bm=" + bm + ", acc=" + acc + ", err=" + err + ", f1=" + f1 + "]";
	}

	public void output(PrintWriter out, Object iter, int reviewedItemsCount) {
		out.println(iter + ", " + reviewedItemsCount + ", " + tp + ", " + fp + ", " + fn + ", " + tn + ", " + tpr + ", "
				+ ppv + ", " + tnr + ", " + bm + ", " + acc + ", " + err + ", " + f1);
	}

	public static void outputHeader(PrintWriter out) {
		out.println("iteration, reviewedItemsCount, tp, fp, fn, tn, tpr, ppv, tnr, bm, acc, err, f1");
	}

}
