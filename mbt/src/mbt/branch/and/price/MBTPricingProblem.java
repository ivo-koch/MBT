package mbt.branch.and.price;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

/***
 * Esta clase se usa únicamente para guardar información del problema de
 * pricing.
 * 
 * 
 * @author ik
 *
 */
public final class MBTPricingProblem extends AbstractPricingProblem<DataModel> {

	public MBTPricingProblem(DataModel dataModel, String name) {
		super(dataModel, name);
	}

//	// Desde acá se inicializa el problema de pricing con las duales del master
//	public void initPricingProblem(double[] duals) {
//		for (int i = 0; i < duals.length; i++)
//			this.dualCosts[i] = duals;
//	}
}
