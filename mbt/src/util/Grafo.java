package util;

import java.util.HashSet;
import java.util.Set;

public class Grafo {

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

	public Set<Integer> getVecinos(int v) {
		Set<Integer> res = new HashSet<Integer>();

		for (int w = 0; w < this.getVertices(); ++w)
			if (v != w && isArista(v, w))
				res.add(w);

		return res;

	}

	public Grafo aumentar(int vertices) {

		Grafo g = new Grafo(this.getVertices() + vertices);

		for (int i = 0; i < this.getVertices(); ++i)
			for (int j = i + 1; j < this.getVertices(); ++j)
				if (this.isArista(i, j))
					g.setArista(i, j);

		return g;

	}

	public int getAristas() {

		int aristas = 0;
		for (int i = 0; i < this.getVertices(); ++i)
			for (int j = i + 1; j < this.getVertices(); ++j)
				if (this.isArista(i, j))
					aristas++;

		return aristas;
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
