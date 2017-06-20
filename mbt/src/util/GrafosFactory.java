package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class GrafosFactory {

	public static Grafo K(int n, int m) {

		Grafo g = new Grafo(n + m);

		for (int i = 0; i < n; i++)
			for (int j = 0; j < m; j++)
				g.setArista(i, j + n);

		return g;
	}

	/***
	 * Generamos un árbol random
	 * 
	 * @param vertices
	 * @param density
	 * @return
	 */
	public static Grafo randomTree(int maxVert, int maxVerticesPorHijo) {

		GrafoBuilder b = new GrafoBuilder();

		Random rand = new Random();
		
		
		List<Integer> vertices = new ArrayList<Integer>();
		
		vertices.add(b.addVertice());
		int indiceActual = 0;
		
		while (b.vertices() < maxVert && indiceActual < vertices.size()) {
			
			int actual = vertices.get(indiceActual);
			
			int hijos = rand.nextInt(maxVerticesPorHijo);
			
			for (int w = 0; w < hijos; w++) {
				int nuevo = b.addVertice();
				b.addArista(actual, nuevo);
				vertices.add(nuevo);
			}
			indiceActual++;
		}
		
		return b.buildGrafo();

	}

	/***
	 * Generamos un grafo random según el modelo de Erdös-Renyi
	 * 
	 * @param vertices
	 * @param density
	 * @return
	 */
	public static Grafo randomGraph(int vertices, int edges) {

		Grafo g = new Grafo(vertices);

		Random rand = new Random();

		int added = 0;
		while (added < edges) {
			int v1 = rand.nextInt(vertices);
			int v2 = rand.nextInt(vertices);
			if (v1 != v2) {
				g.setArista(v1, v2);
				added++;
			}
		}

		return g;
	}

	public static Grafo randomGraph(int vertices, double density) {

		int edges = (int) Math.round(density * vertices * (vertices - 1) / 2);
		return GrafosFactory.randomGraph(vertices, edges);
	}

}
