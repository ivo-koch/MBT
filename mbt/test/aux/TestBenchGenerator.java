package aux;

import java.io.FileNotFoundException;
import java.io.IOException;

import util.Grafo;
import util.GraphUtils;

public class TestBenchGenerator {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		String pathSalida = "./rand/";
		int cantAGenerar = 50;

		double[] dens = { 0.2, 0.4, 0.6, 0.8 };
		for (int i = 6; i < cantAGenerar; i++) {
			for (double d : dens) {
				Grafo g = GraphGenerator.randomGraph(10 + i, d);
//int upperBound = BColUpperBound.upperBound(g);

				//String addenda = "c = " + upperBound + ";\n";
				// addenda += "req = " + Arrays.toString(req) + ";\n";

				GraphUtils.saveToTxt(pathSalida + "G_" + (10 + i) + "_" + d, g);
			}

		}

	}

}
