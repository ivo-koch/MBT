package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchAndPrice;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import util.Grafo;

/**
 * Clase principal del branch and price.
 */
public final class MBTBranchAndPrice extends AbstractBranchAndPrice<DataModel, MBTColumn, MBTPricingProblem> {

    public MBTBranchAndPrice(DataModel dataModel,
                          AbstractMaster<DataModel, MBTColumn, MBTPricingProblem, ? extends MasterData> master,
                          MBTPricingProblem pricingProblem,
                          List<Class<? extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem>>> solvers,
                          List<? extends AbstractBranchCreator<DataModel, MBTColumn, MBTPricingProblem>> abstractBranchCreators,
                          double lowerBoundOnObjective,
                          double upperBoundOnObjective) {
        super(dataModel, master, pricingProblem, solvers, abstractBranchCreators, lowerBoundOnObjective, upperBoundOnObjective);
    }

    /**
     * Generates an artificial solution. Columns in the artificial solution are of high cost such that they never end up in the final solution
     * if a feasible solution exists, since any feasible solution is assumed to be cheaper than the artificial solution. The artificial solution is used
     * to guarantee that the master problem has a feasible solution.
     * Generamos una solucion inicial.
     *
     * @return artificial solution
     */
    @Override
    protected List<MBTColumn> generateInitialFeasibleSolution(BAPNode<DataModel, MBTColumn> node) {
        List<MBTColumn> artificialSolution=new ArrayList<>();
                          
        //y ahora hacemos BFS desde ese V0.
        Arbol bfs = dataModel.getGrafo().bfs(dataModel.getV0());
               
        for (int v: bfs.getHijos(bfs.getRoot()))          
        	artificialSolution.add(new MBTColumn(pricingProblems.get(0), true, "generateInitialFeasibleSolution", Arbol.Builder.buildSubarbol(bfs, v)));
        
        return artificialSolution;
    }

    /**
     * Chequea si la solución del nodo actual es entera. 
     * Una solución es entera si todo vértice aparece en exactamente un árbol
     * */
    @Override
    protected boolean isIntegerNode(BAPNode<DataModel, MBTColumn> node) {
        int vertexCount=0;
        for(MBTColumn column : node.getSolution())
            vertexCount+= column.getArbol().getInternalNodes().size() + 1; //la raíz está afuera del conj de vértices.
        return vertexCount==dataModel.getGrafo().getVertices();
    }
}
