package mbt.branch.and.price;

import java.util.Collections;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

/***
 * Esta clase se usa únicamente para guardar información del problema de
 * pricing.
 * 
 * @author ik
 *
 */
public final class MBTPricingProblem extends AbstractPricingProblem<DataModel> {

	public MBTPricingProblem(DataModel dataModel, String name) {
		super(dataModel, name);
	}

	//private Set<Integer> V0 = null;

	//private int[] offset = null;

//	// El V0 es sólo lectura.
//	public Set<Integer> getV0() {
//		return Collections.unmodifiableSet(V0);
//	}
//
//	// Lo hacemos así para no permitir una modificación del offset acá.
//	public int getOffset(int vertice) {
//		return offset[vertice];
//	}

	// Desde acá se inicializa el problema de pricing.
	public void initPricingProblem(double[] duals) {
//		this.V0 = V0;
//		this.offset = offset;
		this.dualCosts = duals;
	}
}
