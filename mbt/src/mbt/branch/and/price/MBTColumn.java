package mbt.branch.and.price;

import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;

/**
 * Definición de una columna/árbol. No está pensada para ser modificada una vez
 * construída.
 *
 */
public final class MBTColumn extends AbstractColumn<DataModel, MBTPricingProblem> {

	private final Arbol arbol;

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
	public MBTColumn(MBTPricingProblem associatedPricingProblem, boolean isArtificial, String creator, Arbol arbol) {
		super(associatedPricingProblem, isArtificial, creator);
		this.arbol = arbol;
	}

//	@Override
//	public String toString() {
//		return "Value: " + this.value + " artificial: " + isArtificialColumn + " set: "
//				+ this.getArbol().getInternalNodes().toString() + " root: v" + this.getArbol().getRoot() + "t : " + this.getArbol().getCosto();
//	}
	
	@Override
	public String toString() {
		return " set: " +this.getArbol().getInternalNodes().toString() + " root: v" + this.getArbol().getRoot() + "t : " + this.getArbol().getCosto();
	}

	public Arbol getArbol() {
		return arbol;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arbol == null) ? 0 : arbol.hashCode());
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
		MBTColumn other = (MBTColumn) obj;
		if (arbol == null) {
			if (other.arbol != null)
				return false;
		} else if (!arbol.equals(other.arbol))
			return false;
		return true;
	}
 
}
