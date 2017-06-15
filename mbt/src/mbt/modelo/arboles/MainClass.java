package mbt.modelo.arboles;


import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import util.Grafo;
import util.GraphRenderer;

public class MainClass {

	public static void main(String[] args) throws Exception {

		Grafo g = new Grafo(8);
				
		g.setArista(0, 1);
		g.setArista(0, 2);
		g.setArista(0, 3);
		g.setArista(0, 4);
		g.setArista(1, 2);
		g.setArista(1, 5);
		g.setArista(2, 5);
		g.setArista(3, 6);
		g.setArista(5, 6);
		g.setArista(5, 7);
		
		g.setPeso(1, 2, -100);
		g.setPeso(1, 5, -100);
		g.setPeso(5, 6, -100);
	
		Modelo m = new Modelo(g, 0, 10);
		
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
