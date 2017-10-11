package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Arbol {

	/** Vértices del árbol **/
	private final int[] vertices;
	/** Raíz del árbol **/
	private final int root;

	/** t del árbol. **/
	private final int costo;

	/***
	 * Nos guardamos por las dudas el valor de la función objetivo de cuando
	 * calculamos este árbol
	 **/
	private final double valorFuncionObjetivo;

	/***
	 * Usamos esto para construir un árbol correcto, con el patrón Builder
	 * 
	 * @author ik
	 *
	 */
	public static class Builder {

		private int[] vertices;
		private int root;

		/***
		 * Constructor para el builder. Este n es el máximo número de vértices que puede
		 * tener el árbol.
		 * 
		 * @param n
		 * @param root
		 */
		public Builder(int n, int root) {

			this.vertices = new int[n];
			for (int i = 0; i < n; i++)
				this.vertices[i] = -1;

			this.vertices[root] = root;
			this.root = root;
		}

		public void addVertex(int v, int parent) {

			if (v >= this.vertices.length)
				throw new RuntimeException(v + " no es un label válido para un vértices. Debe estar entre 0 y "
						+ (this.vertices.length - 1));

			if (this.vertices[parent] == -1)
				throw new RuntimeException(parent + " no está en el árbol.");

			if (this.vertices[v] > -1)
				throw new RuntimeException(v + "ya está en el árbol");

			this.vertices[v] = parent;
		}

		public boolean contains(int v) {
			return this.vertices[v] > -1;
		}

		public Arbol buildArbol() {

			return new Arbol(this.vertices, this.root);
		}

		public Arbol buildArbol(int costo, double valorFuncionObjetivo) {

			return new Arbol(this.vertices, this.root, costo, valorFuncionObjetivo);
		}

		public static Arbol buildSubarbol(Arbol arbol, int raizSubarbol) {

			int[] newVertices = new int[arbol.vertices.length];
			List<Integer> bfs = arbol.bfs(raizSubarbol);

			for (int i = 0; i < arbol.vertices.length; i++)
				newVertices[i] = -1;

			for (int v : bfs)
				newVertices[v] = arbol.vertices[v];

			newVertices[raizSubarbol] = raizSubarbol;
			return new Arbol(newVertices, raizSubarbol);

		}
	}

	/***
	 * Crea un árbol a partir de un arreglo y su raíz.
	 * 
	 * @param vertices
	 * @param root
	 */
	private Arbol(int[] vertices, int root) {

		this.vertices = vertices;
		this.root = root;
		this.costo = calcularCosto();
		this.valorFuncionObjetivo = Double.MAX_VALUE;
	}

	/****
	 * Crea un árbol, pero le informamos el costo (si no lo informamos, lo calcula)
	 * 
	 * @param vertices
	 * @param root
	 * @param costo
	 */
	private Arbol(int[] vertices, int root, int costo, double valorFuncionObjetivo) {

		this.vertices = vertices;
		this.root = root;
		this.costo = costo;
		this.valorFuncionObjetivo = valorFuncionObjetivo;
	}

	/***
	 * Implementación estándar de bfs. Devuelve los nodos en bfs a partir del
	 * indicado. En un mismo nivel, va a devolver siempre en orden lexicográfico los
	 * vértices.
	 * 
	 * @param v
	 * @return
	 */
	public List<Integer> bfs(int v) {

		List<Integer> res = new ArrayList<Integer>();

		boolean visited[] = new boolean[this.vertices.length];

		LinkedList<Integer> queue = new LinkedList<Integer>();

		visited[v] = true;
		queue.add(v);

		while (queue.size() != 0) {
			// Desencolamos el primero de la cola.
			int s = queue.poll();
			res.add(s);

			List<Integer> hijos = getHijos(s);

			for (int h : hijos) {
				if (!visited[h]) {
					visited[h] = true;
					queue.add(h);
				}
			}
		}
		return res;
	}

	/***
	 * Calcula el costo de un árbol.
	 * 
	 * @return
	 */
	public int calcularCosto() {

		int n = this.vertices.length;

		int tV[] = new int[n];

		List<Integer> bfs = bfs(this.root);

		// recorremos el arbol bottom up, desde el fin de la lista de bfs hasta el
		// principio.
		for (int i = bfs.size() - 1; i >= 0; i--) {
			int v = bfs.get(i);

			List<Integer> Nv = getHijos(v);
			if (!Nv.isEmpty()) { // las hojas están en 0 por default.
				int[] tHijos = new int[Nv.size()];
				int j = 0;
				for (int w : Nv)
					tHijos[j++] = tV[w];

				Arrays.sort(tHijos);
				for (int k = 1; k <= tHijos.length; k++)
					tV[v] = Math.max(tV[v], k + tHijos[tHijos.length - k]);
			}
		}

		return tV[root];
	}

	public boolean contains(int v) {
		return this.vertices[v] > -1;
	}

	private Set<Integer> vertexSet;

	/***
	 * Devuelve los nodos internos de este árbol
	 * 
	 * @return
	 */
	public Set<Integer> getInternalNodes() {

		if (vertexSet == null) {
			vertexSet = new HashSet<Integer>();
			for (int v = 0; v < this.vertices.length; v++)
				if (contains(v) && v != root)
					vertexSet.add(v);
		}

		return Collections.unmodifiableSet(vertexSet);

	}

	public List<Integer> getHijos(int v) {

		List<Integer> hijos = new ArrayList<Integer>();
		for (int w = 0; w < this.vertices.length; w++)
			if (this.vertices[w] == v && w != v)
				hijos.add(w);

		return hijos;

	}

	public int parent(int v) {
		return this.vertices[v];
	}

	public int getRoot() {
		return root;
	}

	public double getCosto() {
		return costo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + root;
		result = prime * result + Arrays.hashCode(vertices);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Arbol other = (Arbol) obj;
		if (root != other.root)
			return false;
		if (!Arrays.equals(vertices, other.vertices))
			return false;
		return true;
	}

	public double getValorFuncionObjetivo() {
		return valorFuncionObjetivo;
	}

}
