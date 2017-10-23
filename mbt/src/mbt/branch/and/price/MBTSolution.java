package mbt.branch.and.price;

import java.util.List;

public class MBTSolution {

	private double objective;
	private int iterations;
	private int nodes;
	private double time;
	private double timePricing;
	private boolean isOptimal;
	private List<MBTColumn> columns;

	public MBTSolution(double objective, int iterations, int nodes, double time, double timePricing, boolean isOptimal,
			List<MBTColumn> columns) {

		this.objective = objective;
		this.iterations = iterations;
		this.time = time;
		this.timePricing = timePricing;
		this.isOptimal = isOptimal;
		this.columns = columns;
	}

	public void print() {
		// TODO Auto-generated method stub

		// Print solution:
		System.out.println("================ Solution ================");
		System.out.println("BAP terminated with objective (MBT): " + objective);
		System.out.println("Total Number of iterations: " + iterations);
		System.out.println("Total Number of processed nodes: " + nodes);
		System.out.println("Total Time spent on master problems: " + time + " Total time spent on pricing problems: "
				+ timePricing);
		// if (bap.hasSolution()) {
		System.out.println("Solution is optimal: " + isOptimal);
		System.out.println("Columns (only non-zero columns are returned):");
		for (MBTColumn column : columns)
			System.out.println(column);
		// }

	}

	public double getObjective() {
		return objective;
	}

	public void setObjective(double objective) {
		this.objective = objective;
	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public int getNodes() {
		return nodes;
	}

	public void setNodes(int nodes) {
		this.nodes = nodes;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public boolean isOptimal() {
		return isOptimal;
	}

	public void setOptimal(boolean isOptimal) {
		this.isOptimal = isOptimal;
	}

	public List<MBTColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<MBTColumn> columns) {
		this.columns = columns;
	}

}
