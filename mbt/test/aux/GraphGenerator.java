package aux;

import java.util.Random;

import util.Grafo;
import util.Grafo.Builder;


public class GraphGenerator {

	
	    /***
	     * Generamos un grafo random según el modelo de Erdös-Renyi
	     * @param vertices
	     * @param density
	     * @return
	     */
		public static Grafo randomGraph(int vertices, int edges) {

			Builder b = new Builder(vertices);			
			
			Random rand = new Random();
			
			int added = 0;
			while (added < edges){
				int v1 = rand.nextInt(vertices);
				int v2 = rand.nextInt(vertices);
				if (v1 != v2 ){
					b.setArista(v1, v2);
					added++;
				}
			}

			return b.build();
		}
		
		
		public static Grafo randomGraph (int vertices, double density) {
			
			int edges = (int) Math.round(density * vertices * (vertices - 1) / 2);  
			return GraphGenerator.randomGraph(vertices, edges);
		}
}
