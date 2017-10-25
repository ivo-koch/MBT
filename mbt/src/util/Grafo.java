package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mbt.branch.and.price.Arbol;

public class Grafo {

	public class AristaDirigida {

		int v1;

		int v2;

		public AristaDirigida(int v1, int v2) {
			this.v1 = v1;
			this.v2 = v2;
		}

		public int getV1() {
			return v1;
		}

		public int getV2() {
			return v2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + v1;
			result = prime * result + v2;
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
			AristaDirigida other = (AristaDirigida) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (v1 != other.v1)
				return false;
			if (v2 != other.v2)
				return false;
			return true;
		}

		private Grafo getOuterType() {
			return Grafo.this;
		}
		
		@Override
		public String toString() {
			return "[" + this.v1 +  ", " + this.v2 + "]";
		}

	}

	public static class Builder {

		private boolean[][] aristas;
		private int n;

		public Builder(int n) {
			this.n = n;
			aristas = new boolean[n][n];
		}

		public Builder setArista(int i, int j) {
			aristas[i][j] = true;
			aristas[j][i] = true;
			return this;
		}

		public boolean isArista(int i, int j) {
			return aristas[i][j];
		}

		public int getN() {
			return n;
		}

		public Grafo build() {
			return new Grafo(this);
		}
	}

	// Constructor
	private Grafo(Builder b) {

		this.n = b.getN();
		this.aristas = new boolean[n][n];

		for (int i = 0; i < n; ++i)
			for (int j = i + 1; j < n; ++j)
				if (b.isArista(i, j))
					this.setArista(i, j);

	}

	public Arbol bfs(Set<Integer> V0) {

		int vn = this.getVertices();
		Arbol.Builder builder = new Arbol.Builder(this.getVertices() + 1, vn);

		boolean visited[] = new boolean[this.getVertices()];

		// Create a queue for BFS
		LinkedList<Integer> queue = new LinkedList<Integer>();

		for (int v0 : V0) {
			// Mark the current node as visited and enqueue it
			visited[v0] = true;
			queue.add(v0);
			builder.addVertex(v0, vn);
		}

		while (queue.size() != 0) {
			// Desencolamos el primero de la cola.
			int s = queue.poll();

			Set<Integer> hijos = getVecinos(s);

			for (int h : hijos) {
				if (!visited[h]) {
					visited[h] = true;
					queue.add(h);
					builder.addVertex(h, s);
				}
			}
		}
		return builder.buildArbol();

	}

	// Cantidad de aristas
	private int n;

	private double[][] pesosAristas;

	private double[] pesosVertices;

	public Grafo(int n) {
		this.n = n;
		this.aristas = new boolean[n][n];
		this.pesosAristas = new double[n][n];
		this.pesosVertices = new double[n];
	}

	public int gradoMaximo() {

		int maxGrado = 0;
		for (int i = 0; i < this.getVertices(); ++i)
			maxGrado = Math.max(maxGrado, this.getVecinos(i).size());

		return maxGrado;
	}

	// Aristas
	private boolean[][] aristas;

	private Map<Integer, List<AristaDirigida>> verticesConAristas = new HashMap<Integer, List<AristaDirigida>>();

	public Set<Integer> getVecinos(int v) {
		Set<Integer> res = new HashSet<Integer>();

		for (int w = 0; w < this.getVertices(); ++w)
			if (v != w && isArista(v, w))
				res.add(w);

		return res;
	}

	public AristaDirigida getArista(int i, int v) {
		for (AristaDirigida arista : getAristasIncidentes(i))
			if (arista.v2 == v)
				return arista;

		throw new RuntimeException("La arista no existe");
	}

	public List<AristaDirigida> getAristasIncidentes(int v) {
		return verticesConAristas.get(v);
	}

	public int getAristas() {

		int aristas = 0;
		for (int i = 0; i < this.getVertices(); ++i)
			for (int j = i + 1; j < this.getVertices(); ++j)
				if (this.isArista(i, j))
					aristas++;

		return aristas;
	}

	public void show() {
		new GraphRenderer(this);
	}

	public double getPeso(int i, int j) {
		checkArista(i, j);
		return pesosAristas[i][j];
	}

	private void checkArista(int i, int j) {
		if (!isArista(i, j))
			throw new IllegalArgumentException("(" + i + ", " + j + ") no es arista, pa.");
	}

	public void setPeso(int i, int j, double peso) {
		checkArista(i, j);

		this.pesosAristas[i][j] = peso;
		this.pesosAristas[j][i] = peso;
	}

	public void setPeso(int i, double peso) {
		this.pesosVertices[i] = peso;
	}

	public double getPeso(int i) {
		return this.pesosVertices[i];
	}

	public boolean isArista(int i, int j) {
		checkRange(i, j);

		return aristas[i][j];
	}

	public void setArista(int i, int j) {
		checkRange(i, j);

		this.aristas[i][j] = true;
		this.aristas[j][i] = true;

		addArista(i, j);
		addArista(j, i);

	}

	private void addArista(int i, int j) {
		List<AristaDirigida> set;

		if (!verticesConAristas.containsKey(i))
			verticesConAristas.put(i, new ArrayList<AristaDirigida>());

		set = verticesConAristas.get(i);

		set.add(new AristaDirigida(i, j));
	}

	private void checkRange(int i, int j) {
		if (i < 0 || i >= n || j < 0 || j >= n || i == j)
			throw new IllegalArgumentException("Par de vértices inválido: (" + i + ", " + j + "), n = " + n);
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append(this.getVertices() + "\n");

		for (int i = 0; i < this.getVertices(); ++i)
			for (int j = i + 1; j < this.getVertices(); ++j)
				if (this.isArista(i, j))
					sb.append(i + ", " + j + "\n");

		return sb.toString();
	}

	// Consultas
	public int getVertices() {
		return n;
	}

}
