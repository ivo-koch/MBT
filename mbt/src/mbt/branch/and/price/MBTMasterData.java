package mbt.branch.and.price;


import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.Map;

import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import util.Grafo;

/**
 * Container which stores information coming from the master problem. It contains:
 * <ul>
 * <li>a reference to the cplex model</li>
 * <li>reference to the pricing problem</li>
 * </ul>
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class MBTMasterData extends MasterData<DataModel, Arbol, MBTPricingProblem, IloNumVar> {

    /** Cplex instance **/
    public final IloCplex cplex;

    /**
     * Creates a new MasterData object
     *
     * @param cplex cplex instance
     * @param varMap A bi-directional map which stores the variables. The first key is the pricing problem, the second key is a column and the value is a variable object, e.g. an IloNumVar in cplex.
     */
    public MBTMasterData(IloCplex cplex,
                              Map<MBTPricingProblem, OrderedBiMap<Arbol, IloNumVar>> varMap) {
        super(varMap);
        this.cplex=cplex;
    }
}
