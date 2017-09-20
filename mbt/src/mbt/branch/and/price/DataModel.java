package mbt.branch.and.price;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

import util.Grafo;

/***
 * Esta clase contiene información del problema, que se comparte entre el master
 * y el problema de pricing.
 * 
 * @author ik
 *
 */
public class DataModel implements ModelInterface {

	/*** El grafo ***/
	private final Grafo grafo;
	/*** El V0, que va a cambiar dinámicamente en el branching **/
	private final Set<Integer> V0;
	/*** El V0 inicial, no cambia nunca. **/
	private final Set<Integer> initialV0;
	/***
	 * El offset para cada V0. Esto va a cambiar dinámicamente en el branching.
	 **/
	private final int[] offset;
	/*** Una cota para el T **/
	private final int maxT;
	/*** El M grande para usar en las desigualdades **/
	private final double M;

	public DataModel(Grafo g, Set<Integer> V0) {
		this.grafo = g;
		this.V0 = new HashSet<Integer>(V0);
		this.initialV0 = new HashSet<Integer>(V0);
		this.offset = new int[this.getGrafo().getVertices()];
		this.maxT = g.getVertices() - 1;
		this.M = Math.pow(g.getVertices(), 3);
	}

	@Override
	public String getName() {		
		return "";
	}

	public Grafo getGrafo() {
		return grafo;
	}

	public Set<Integer> getV0() {
		return V0;
	}

	public Set<Integer> getInitialV0() {
		return Collections.unmodifiableSet(initialV0);
	}

	public int getMaxT() {
		return maxT;
	}

	public int[] getOffset() {
		return offset;
	}

	public double getM() {
		return M;
	}

}
