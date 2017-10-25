package comparacion;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import mbt.branch.and.price.MBTSolution;
import mbt.branch.and.price.MBTSolver;
import mbt.modelo.compacto.variables.x.Modelo;
import util.Grafo;
import util.GraphUtils;

public class ModeloCompactoVsPricing {

	

	@Test
	public void testModelos() throws Exception {

		Writer writer = new FileWriter("logGeneral");
		// Writer writer = new BufferedWriter(new OutputStreamWriter(new
		// FileOutputStream("./tester-logs/logGeneral"), "utf-8"));
		String directory = "./testInst";

		DirectoryStream<Path> ds = Files.newDirectoryStream(FileSystems.getDefault().getPath(directory));

		List<Path> archivos = new ArrayList<Path>();

		for (Path p : ds)
			archivos.add(p);

		Collections.sort(archivos);

		for (Path p : archivos) {

			if (Files.isDirectory(p))
				continue;
			// Iteramos por todos los archivos del directorio y resolvemos
			String nombreGrafo = p.getFileName().toString();

			String fullPath =directory + FileSystems.getDefault().getSeparator() + nombreGrafo;
			Grafo g = GraphUtils.loadFromTxt(fullPath);

			FileOutputStream out = null;
			try {

				Set<Integer> V0 = GraphUtils.leerV0(fullPath);
				
				Modelo modeloCompacto = new Modelo(g, V0);
				modeloCompacto.solve(new ByteArrayOutputStream());

				MBTSolver modeloBranchAndPrice = new MBTSolver(g, V0);

				//MBTSolution sol = modeloBranchAndPrice.solve();

				//assertEquals(modeloCompacto.getSolucion() + 1, sol.getObjective(), 0.0001);

			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {

				if (out != null)
					out.close();
				// System.gc();
			}

		}

		writer.close();
	}
}
