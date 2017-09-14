package mbt.branch.and.price;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

import util.Grafo;

public class DataModel implements ModelInterface{
	
	
	protected Grafo grafo;
	protected TreeSet<Integer> V0;
	protected int maxT;
	
	protected Map<Integer, Integer> offset = new HashMap<Integer, Integer>();
	
	protected double M;

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
