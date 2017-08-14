package mbt.modelos.comparacion;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import util.Grafo;
import util.GrafosFactory;
import util.GraphUtils;

public class TestModelo {

	@Test
	public void testArboles() throws Exception {

		// testea si el resultado del costo de un árbol usando prog dinámica es
		// el mismo del modelo.

		// for (int maxVertices = 10; maxVertices < 200; maxVertices +)
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("resultadosComparacion"), "utf-8"))) {

			for (int i = 50; i < 75; i += 5) {
				for (int j = 5; j < i; j += 10) {
					Grafo g = GrafosFactory.randomTree(i, j);

					if (g.getVertices() > 1) {
						for (int v = 0; v < g.getVertices(); v++)
							g.setPeso(v, 10);

						System.out.println("Fin generación grafo G " + i + " n:" + g.getVertices() + " maxHijos: " + g.getAristas());
						GraphUtils.saveToTxt("G" + i, g);

						Set<Integer> V0 = new HashSet<Integer>();
						V0.add(0);

						mbt.modelo.compacto.variables.x.Modelo modeloCompacto = new mbt.modelo.compacto.variables.x.Modelo(g, V0);

						mbt.modelo.arboles.Modelo modeloArboles = new mbt.modelo.arboles.Modelo(g, 0);

						modeloCompacto.solve(new ByteArrayOutputStream());

						writer.append("Arbol: " + g.getVertices() + "| maxHijos:" + j);
						writer.append("|Tiempo modelo compacto: " + modeloCompacto.getTiempoEjecucion());

						modeloArboles.solve(new ByteArrayOutputStream());

						writer.append("|Tiempo modelo arboles: " + modeloArboles.getTiempoEjecucion());

						System.out.println("Fin solver");

						int solucion = calcularT(g);
						writer.append("|solucion: " + solucion + "\n");
						assertEquals(modeloCompacto.getSolucion() + 1, solucion);

						assertEquals(modeloArboles.getSolucion()[0], solucion);
					}
				}
			}
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
