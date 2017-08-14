package mbt.modelo.arboles;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import util.Grafo;
import util.GrafosFactory;
import util.GraphUtils;

public class TestModelo {

	@Test
	public void testGrafo1() throws Exception {
		// probamos con el grafo del viernes 16.07
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

		Modelo m = new Modelo(g, 0);

		m.solve(new ByteArrayOutputStream());

		assertEquals(m.getSolucion()[0], 6);
	}

	@Test
	public void testGrafo2() throws Exception {

		// probamos con un wheel
		Grafo g = new Grafo(6);

		g.setArista(0, 1);
		g.setArista(0, 2);
		g.setArista(0, 3);
		g.setArista(0, 4);
		g.setArista(0, 5);
		g.setArista(1, 2);
		g.setArista(2, 3);
		g.setArista(3, 4);
		g.setArista(4, 5);

		Modelo m = new Modelo(g, 0);

		m.solve(new ByteArrayOutputStream());

		assertEquals(m.getSolucion()[0], 5);
	}

	@Test
	public void testGrafo3() throws Exception {

		// probamos con otro grafo
		Grafo g = new Grafo(10);

		g.setArista(0, 1);
		g.setArista(0, 2);
		g.setArista(0, 3);
		g.setArista(4, 5);
		g.setArista(5, 6);
		g.setArista(6, 8);
		g.setArista(6, 7);
		g.setArista(7, 8);
		g.setArista(7, 9);
		g.setArista(8, 3);
		g.setArista(3, 7);
		g.setArista(1, 7);
		g.setArista(2, 7);

		Modelo m = new Modelo(g, 0);

		m.solve(new ByteArrayOutputStream());

		assertEquals(m.getSolucion()[0], 7);
	}

	// @Test
	public void testArbol() throws Exception {

		// testea si el resultado del costo de un árbol usando prog dinámica es
		// el mismo del modelo.

		Grafo g = GrafosFactory.randomTree(10, 4);

		Modelo m = new Modelo(g, 0);

		m.solve(new ByteArrayOutputStream());

		assertEquals(m.getSolucion()[0], calcularT(g));
	}

	// @Test
	public void testCalcularT() throws Exception {

		Grafo g = GraphUtils.loadFromTxt("G11_ver");

		Modelo m = new Modelo(g, 0);

		m.solve(new ByteArrayOutputStream());

		assertEquals(m.getSolucion()[0], calcularT(g));
	}

	@Test
	public void testArboles() throws Exception {

		// testea si el resultado del costo de un árbol usando prog dinámica es
		// el mismo del modelo.

		// for (int maxVertices = 10; maxVertices < 200; maxVertices +)
		for (int i = 50; i < 200; i++)
			for (int j = 1; j < i; j++) {
				Grafo g = GrafosFactory.randomTree(i, j);

				for (int v = 0; v < g.getVertices(); v++)
					g.setPeso(v, 10);

				System.out.println("Fin generación grafo G " + i + " n:" + g.getVertices() + " m: " + g.getAristas());
				//GraphUtils.saveToTxt("G" + i, g);
				Modelo m = new Modelo(g, 0);

				m.solve(new ByteArrayOutputStream());

				System.out.println("Fin solver");
				assertEquals(m.getSolucion()[0], calcularT(g));
			}
	}

	/***
	 * Calculamos el t de un árbol usando programación dinámica arbol tiene que
	 * ser un árbol y los vértices tienen que venir numerados en bfs.
	 * 
	 * */
	public static int calcularT(Grafo arbol) {

		// computamos el v para cada hijo.
		int tV[] = new int[arbol.getVertices()];

		for (int v = arbol.getVertices() - 1; v >= 0; v--) {
			Set<Integer> Nv = arbol.getVecinos(v);
			if (Nv.size() == 1 && v != 0) // es una folha
				tV[v] = 0;
			else {
				int[] tHijos = new int[v == 0 ? Nv.size() : (Nv.size() - 1)];
				int i = 0;
				for (int w : Nv)
					if (w > v) // excluimos al padre
						tHijos[i++] = tV[w];

				Arrays.sort(tHijos);
				for (int k = 1; k <= tHijos.length; k++)
					tV[v] = Math.max(tV[v], k + tHijos[tHijos.length - k]);
			}
		}

		return tV[0];
	}

}
