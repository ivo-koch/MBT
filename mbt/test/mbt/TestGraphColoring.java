package mbt;

import java.io.IOException;

import org.jorlib.demo.frameworks.columnGeneration.graphColoringBAP.ChromaticNumberCalculator;
import org.jorlib.demo.frameworks.columnGeneration.graphColoringBAP.model.ColoringGraph;
import org.junit.Test;

public class TestGraphColoring {

	@Test
	public void test() throws IOException {
		
		//ColoringGraph instancia = new ColoringGraph("./dimacs/DSJC125.1.col");
		ColoringGraph instancia = new ColoringGraph("./dimacs/clique.col");
		ChromaticNumberCalculator solver = new ChromaticNumberCalculator(instancia);		
	}

}
