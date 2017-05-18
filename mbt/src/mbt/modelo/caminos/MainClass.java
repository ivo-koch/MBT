package mbt.modelo.caminos;


import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class MainClass {

	public static void main(String[] args) throws Exception {

		Grafo g = new Grafo(7);
				
		g.setArista(0, 2);
		g.setArista(1, 3);
		g.setArista(2, 3);
		g.setArista(2, 4);
		g.setArista(3, 4);
		g.setArista(3, 5);
		g.setArista(4, 6);
		
		
		Set<Integer> V0 = new HashSet<Integer>();
		
		V0.add(0);
		V0.add(1);
		
		Modelo m = new Modelo(g, V0, 10);
		
		m.solve(new ByteArrayOutputStream());
		new GraphRenderer(g, m.getSolucion());
		
//		
//		for (int size = 300; size < 900; size += 100) {
//			for (int dens = 1; dens < 10; dens+=2) {
//				try {
//
//					System.out.println("Size " + size + "Density: " + dens / 10.0);
//					Modelo m = new Modelo(GrafosFactory.randomGraph(size, dens / 10.0));
//
//					m.solve(new ByteArrayOutputStream());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}

		// new GraphRenderer(g);

	}

}
