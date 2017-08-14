package util;

public class MostrarGrafoMain {

	public static void main(String[] args) {
		
		Grafo g = GrafosFactory.randomTree(30,20);
		g.show();		

	}

}
