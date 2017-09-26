package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.List;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;

import util.Grafo.AristaDirigida;

/**
 * Clase responsable de crear los branches.
 * 
 */
public final class MBTBranchCreator extends AbstractBranchCreator<DataModel, MBTColumn, MBTPricingProblem> {

	public MBTBranchCreator(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
	}

	private int v0ConMultiplesArboles = -1;

	/**
	 * Podemos hacer un branching si hay un v fuera de V0.
	 */
	@Override
	protected boolean canPerformBranching(List<MBTColumn> solution) {

		v0ConMultiplesArboles = -1;
		// Buscamos el árbol fraccionario.
		// Para eso, la única forma que tenemos de verlo desde acá es viendo los árboles
		// que salgan del
		// mismo v0
		boolean[] v0YaEncontrado = new boolean[dataModel.getGrafo().getVertices()];
		for (MBTColumn columna : solution) {
			if (v0YaEncontrado[columna.getArbol().getRoot()]) {
				v0ConMultiplesArboles = columna.getArbol().getRoot();
				break;
			}
			v0YaEncontrado[columna.getArbol().getRoot()] = true;
		}

		return v0ConMultiplesArboles > -1;
	}

	/**
	 * Creamos los branches
	 * 
	 * 
	 * @param parentNode
	 *            Nodo fraccionario a partir del cual brancheamos.
	 * 
	 * @return Lista de hijos para el branch.
	 */
	@Override
	protected List<BAPNode<DataModel, MBTColumn>> getBranches(BAPNode<DataModel, MBTColumn> parentNode) {

		List<BAPNode<DataModel, MBTColumn>> branches = new ArrayList<BAPNode<DataModel, MBTColumn>>();
	
		//notar que usamos el v0 a partir del cual salen múltiples árboles 
		//que identificamos en el canPerformBranching
		
		// Creamos un branch para cada arista desde el v0 para el cual decidimos branchear.
		for (AristaDirigida arista : dataModel.getGrafo().getAristasIncidentes(v0ConMultiplesArboles))
			if (!this.dataModel.getV0().contains(arista.getV2())) {
				MBTBranchingDecision bd = new MBTBranchingDecision(arista);
				BAPNode<DataModel, MBTColumn> node = this.createBranch(parentNode, bd, parentNode.getSolution(),
						parentNode.getInequalities());
				branches.add(node);
			}

		return branches;
	}
}
