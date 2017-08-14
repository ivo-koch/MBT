package util;

import java.util.HashSet;
import java.util.Set;


public class GrafoBuilder {
	
	private class Arista {
		private int i;
		private int j;

		public Arista(int i, int j) {
			this.i = i;
			this.j = j;
		}

	
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "(" + i + "," + j + ")";
		}
	}

	private Set<Arista> aristas = new HashSet<GrafoBuilder.Arista>(); 
	
	private int maxVAgregado = -1;
	
	public int addVertice() {

		maxVAgregado++;
		return maxVAgregado;
	}
	
	public int vertices() {
		
		return maxVAgregado + 1;
	}
	
	
	public void addArista(int i, int j) {
		
		if (i > maxVAgregado)
			throw new RuntimeException("Falta agregar el vértice " + i + " al grafo");
		
		if (j > maxVAgregado)
			throw new RuntimeException("Falta agregar el vértice " + j + " al grafo");
		
		aristas.add(new Arista(i, j));
	}
	
	public Grafo buildGrafo() {
		
		Grafo g = new Grafo(this.vertices());
		
		for(Arista a : this.aristas)
			g.setArista(a.i, a.j);
		
		return g; 
	}
			
}