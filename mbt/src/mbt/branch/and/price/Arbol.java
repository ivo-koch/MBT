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
	private int costo;

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
	public Arbol(int n, int root) {

		this.vertices = new int[n];
		for (int i = 0; i < n; i++)
			this.vertices[i] = -1;
		this.vertices[root] = root;
		this.root = root;
		this.costo = 0;
		this.valorFuncionObjetivo = Double.MAX_VALUE;
	}

	 /*** Crea un árbol a partir de un arreglo y su raíz.
	 * 
	 * @param vertices
	 * @param root
	 */
	private Arbol(int[] vertices, int root) {

		this.vertices = vertices;
		this.root = root;
		recalcularCosto();
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
	
	public Arbol clonar()  {
		return new Arbol(this.vertices.clone(), this.root, this.getCosto(), this.getValorFuncionObjetivo()); 
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
	public int getCosto() {
		return this.costo;
	}

	public void recalcularCosto() {
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

		this.costo = tV[root];
	}

	public boolean contains(int v) {
		return this.vertices[v] > -1;
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
		recalcularCosto();
	}
	
	public void removeVertex(int v) {

		if (this.vertices[v] == -1)
			throw new RuntimeException(v + " no está en el árbol.");

		if (!this.getHijos(v).isEmpty())
			throw new RuntimeException(v + "tiene hijos en el árbol");

		this.vertices[v] = -1;
		recalcularCosto();
	}
	
	/***
	 * Devuelve los nodos internos de este árbol
	 * 
	 * @return
	 */
	public Set<Integer> getInternalNodes() {

		Set<Integer> vertexSet = new HashSet<Integer>();
			for (int v = 0; v < this.vertices.length; v++)
				if (contains(v) && v != root)
					vertexSet.add(v);

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

//	public void setCosto(double costo) {
//		this.costo = costo;
//	}

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
	
	@Override
	public String toString() 
	{
		return " set: " +this.getInternalNodes().toString() + " root: v" + this.getRoot() + " t : " + this.getCosto();
	}

	public boolean isArista(Integer u, Integer v) 
	{
		return this.parent(u) == v || this.parent(v) == u;
	}

	/**
	 * Devuelve el �nico ciclo del arbol que se forma agregando la arista uv.
	 * Precondici�n: la arista uv no est� en el �rbol
	 * @param u
	 * @param v
	 * @return
	 */
	public List<Integer> getCiclo(Integer u, Integer v) 
	{
		List<Integer> cicloU = new ArrayList<Integer>();
		
		/*
		 * Vamos desde u hasta la ra�z. Si en alg�n momento nos 
		 * encontramos con v, se cerr� el ciclo.
		 */
		cicloU.add(u);
		int p = this.parent(u);
		while (p != this.getRoot())
		{
			cicloU.add(p);
			if (p == v)
				return cicloU;
			
			p = this.parent(p);
		}
		
		
		/*
		 * Vamos desde v hasta la ra�z. Si en alg�n momento nos 
		 * encontramos con u, se cerr� el ciclo. Si no, si nos 
		 * encontramos con un vertice que est� en el camino de U, 
		 * se cierra tambi�n descartando el resto del camino de U.
		 * Si no, entonces el ciclo es ir de U hasta la ra�z y bajar 
		 * hasta V.   
		 */
		List<Integer> cicloV = new ArrayList<Integer>();
		cicloV.add(v);
		p = this.parent(v);
		while (p != this.getRoot())
		{
			cicloV.add(p);
			if (p == u)
				return cicloV;
			
			if (cicloU.contains(p))
			{
				List<Integer> otra = cicloU.subList(0, cicloU.indexOf(p));
				Collections.reverse(otra);
				cicloV.addAll(otra);
				return cicloV;
			}
			
			p = this.parent(p);
		}
		
		// Los dos llegaron hasta la raiz
		
		Collections.reverse(cicloU);
		cicloU.remove(0); // borro la ra�z porque est� repetida
		cicloV.addAll(cicloU);
		return cicloV;
	}

	/**
	 * Cambia el parent de un nodo u por v, siempre que u no sea un descendiente de v.
	 * Devuelve true si hizo el camio.
	 * @param u
	 * @param v
	 */
	public boolean cambiarParent(Integer u, Integer v) 
	{
		int p = this.parent(v);
		
		while (p != this.getRoot())
		{
			if (p == u)
				return false;
			
			p = this.parent(p);
		}

		if (p == u)
			return false;

		this.vertices[u] = v;
		recalcularCosto();
		return true;
	}
	/***
	 * Agrega el camino indicado del árbol. 
	 * Precondición: asumimos que la lista de vértices tiene como último vértice un 
	 * elemento de T.
	 * @param camino
	 */
	public void addPath(List<Integer> camino) {
		int padre = camino.get(camino.size() - 1);
		for (int j = camino.size() - 2; j >= 0; j--) {
			int v = camino.get(j);
			this.addVertex(v, padre);
			padre = v;
		}
	}

	/***
	 * Elimina el camino indicado del árbol.
	 * Precondición: El camino debe ser inducido.
	 * @param camino
	 */
	public void removePath(List<Integer> camino) {
		for (int j = 0; j < camino.size() - 1; j++)
			this.removeVertex(camino.get(j));
	}
	
	/***
	 * Devuelve el valor de la funcion objetivo para el arbol T y el coeficiente
	 * para t dado.
	 * 
	 * @param T
	 * @param coefT
	 * @return
	 */
	public double valorFuncionObjetivo(double coefT, double[] duals, DataModel dataModel) {
		
		double sumaVertices = duals[dataModel.getV0().size() + this.getRoot()];

		for (int v : this.getInternalNodes())
			sumaVertices += duals[dataModel.getV0().size() + v];

		double fObj = coefT * (this.getCosto() + dataModel.getOffset()[this.getRoot()]) + sumaVertices;

		return fObj;
	}

	

}
