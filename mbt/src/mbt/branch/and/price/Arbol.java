package mbt.branch.and.price;

import java.util.Set;

import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;

import util.Grafo;

/**
 * Definition of a column.
 *
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class Arbol extends AbstractColumn<DataModel, MBTPricingProblem>{

    /** Vertices in the independent set **/
    public final Set<Integer> vertices;
    public int root;
    /** Cost of this column in the objective of the Master Problem **/
    public final double cost;

    /**
     * Constructs a new column
     *
     * @param associatedPricingProblem Pricing problem to which this column belongs
     * @param isArtificial             Is this an artificial column?
     * @param creator                  Who/What created this column?
     * @param vertices Vertices in the independent set
     * @param cost cost of the independent set
     */
    public Arbol(MBTPricingProblem associatedPricingProblem, boolean isArtificial, String creator, Set<Integer> vertices, double cost) {
        super(associatedPricingProblem, isArtificial, creator);
        this.vertices=vertices;
        this.cost=cost;
    }


    @Override
    public boolean equals(Object o) {
        if(this==o)
            return true;
        else if(!(o instanceof Arbol))
            return false;
        Arbol other=(Arbol) o;
        return this.vertices.equals(other.vertices) && this.isArtificialColumn == other.isArtificialColumn;
    }

    @Override
    public int hashCode() {
        return vertices.hashCode();
    }

    @Override
    public String toString() {
        return "Value: "+ this.value+" artificial: "+isArtificialColumn+" set: "+vertices.toString();
    }

}
