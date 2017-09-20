package mbt.branch.and.price;

import java.util.Collections;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;

/**
 * Definición de una columna/árbol.
 * No está pensada para ser modificada una vez construída.
 *
 */
public final class Arbol extends AbstractColumn<DataModel, MBTPricingProblem> {

	/** Vértices del árbol **/
	private final Set<Integer> vertices;
	/** Raíz del árbol **/
	private final int root;

	/** t del árbol. **/
	private final double t;

	/**
	 * Construye una nueva columna, que será un árbol
	 *
	 * @param associatedPricingProblem
	 *            Pricing problem to which this column belongs
	 * @param isArtificial
	 *            Is this an artificial column?
	 * @param creator
	 *            Who/What created this column?
	 * @param vertices
	 *            Vertices in the independent set
	 * @param cost
	 *            cost of the independent set
	 */
	public Arbol(MBTPricingProblem associatedPricingProblem, boolean isArtificial, String creator,
			Set<Integer> vertices, int root, double cost) {
		super(associatedPricingProblem, isArtificial, creator);
		this.vertices = vertices;
		this.t = cost;
		this.root = root;
	}

	@Override
	public String toString() {
		return "Value: " + this.value + " artificial: " + isArtificialColumn + " set: " + vertices.toString()
				+ " root: v" + root;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(t);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + root;
		result = prime * result + ((vertices == null) ? 0 : vertices.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Arbol other = (Arbol) obj;
		if (Double.doubleToLongBits(t) != Double.doubleToLongBits(other.t))
			return false;
		if (root != other.root)
			return false;
		if (vertices == null) {
			if (other.vertices != null)
				return false;
		} else if (!vertices.equals(other.vertices))
			return false;
		return true;
	}

	public Set<Integer> getVertices() {
		return Collections.unmodifiableSet(vertices);
	}

	public int getRoot() {
		return root;
	}

	public double getT() {
		return t;
	}

}
