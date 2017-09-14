package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jorlib.demo.frameworks.columnGeneration.graphColoringBAP.cg.ChromaticNumberPricingProblem;
import org.jorlib.demo.frameworks.columnGeneration.graphColoringBAP.cg.IndependentSet;
import org.jorlib.demo.frameworks.columnGeneration.graphColoringBAP.model.ColoringGraph;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchAndPrice;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import util.Grafo;

/**
 * Branch-and-Price implementation
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class BranchAndPrice extends AbstractBranchAndPrice<DataModel, Arbol, MBTPricingProblem> {

    public BranchAndPrice(DataModel dataModel,
                          AbstractMaster<DataModel, Arbol, MBTPricingProblem, ? extends MasterData> master,
                          MBTPricingProblem pricingProblem,
                          List<Class<? extends AbstractPricingProblemSolver<DataModel, Arbol, MBTPricingProblem>>> solvers,
                          List<? extends AbstractBranchCreator<DataModel, Arbol, MBTPricingProblem>> abstractBranchCreators,
                          double lowerBoundOnObjective,
                          double upperBoundOnObjective) {
        super(dataModel, master, pricingProblem, solvers, abstractBranchCreators, lowerBoundOnObjective, upperBoundOnObjective);
    }

    /**
     * Generates an artificial solution. Columns in the artificial solution are of high cost such that they never end up in the final solution
     * if a feasible solution exists, since any feasible solution is assumed to be cheaper than the artificial solution. The artificial solution is used
     * to guarantee that the master problem has a feasible solution.
     *
     * @return artificial solution
     */
    @Override
    protected List<Arbol> generateInitialFeasibleSolution(BAPNode<DataModel, Arbol> node) {
        List<Arbol> artificialSolution=new ArrayList<>();
//        for(int v=0; v<dataModel.getNrVertices(); v++){
//            artificialSolution.add(new IndependentSet(pricingProblems.get(0), true, "Artificial", new HashSet<>(Collections.singletonList(v)), objectiveIncumbentSolution));
//        }
        return artificialSolution;
    }

    /**
     * Checks whether the given node is integer. A solution is integer if every vertex is contained in exactly 1 independent set,
     * that is, if every vertex is assigned a single color.
     * @param node Node in the Branch-and-Price tree
     * @return true if the solution is an integer solution
     */
    @Override
    protected boolean isIntegerNode(BAPNode<DataModel, Arbol> node) {
        int vertexCount=0;
        for(Arbol column : node.getSolution())
            vertexCount+= column.vertices.size();
        return vertexCount==dataModel.grafo.getVertices();
    }
}
