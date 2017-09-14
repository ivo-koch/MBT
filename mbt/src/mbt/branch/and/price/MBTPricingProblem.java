package mbt.branch.and.price;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

import util.Grafo;

/***
 * Esta clase se usa únicamente para guardar información del problema de pricing. 
 * @author ik
 *
 */
public final class MBTPricingProblem extends AbstractPricingProblem<DataModel> {
    
    public MBTPricingProblem(DataModel dataModel, String name) {
        super(dataModel, name);
    }
}
