package mbt.modelo.compacto.variables.x;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import util.Grafo;
import util.GrafosFactory;

public class MainPruebitaRapida {

	public static void main(String[] args) throws Exception {


		Grafo g = GrafosFactory.randomTree(30, 5);
		Set<Integer> V0 = new HashSet<Integer>();
		
		V0.add(0);
		V0.add(1);
		V0.add(2);
		V0.add(3);
		V0.add(4);
		System.out.println("n:" + g.getVertices());
		Modelo m = new Modelo(g, V0);
		

		m.solve(new ByteArrayOutputStream());
		

	}

}
