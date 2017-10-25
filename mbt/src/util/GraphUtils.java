package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;



/**
 * Graph utility class.
 * 
 * @author ik
 * 
 */
public abstract class GraphUtils {

	/**
	 * Saves a graph to a txt file.
	 * 
	 * @param fileName
	 * @param graph
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void saveToTxt(String fileName, Grafo graph) throws UnsupportedEncodingException, FileNotFoundException, IOException {

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"))) {
			writer.write(graph.toString());
		}
	}

	public static void saveToTxt(String fileName, Grafo graph, String addenda) throws UnsupportedEncodingException, FileNotFoundException,
			IOException {

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"))) {
			writer.write(addenda + "\n");
			writer.write(graph.toString());
		}
	}
	
	public static Set<Integer> leerV0(String fileName) throws NumberFormatException, IOException {

		File inputFile = new File(fileName);
		RandomAccessFile in = new RandomAccessFile(inputFile, "r");

		String line;
		Set<Integer> V0 = new HashSet<Integer>();

		Grafo g = null;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("#V0")) {

				String[] elementos = line.split(":");
				boolean first = true;
				for(String elem: elementos) {
					if (first) {
						first = false;
						continue;
					}
					if (!elem.isEmpty())
						V0.add(Integer.parseInt(elem.trim()));
				}
				break;
			}
			

		}
		in.close();

		return V0;
	}
	

	/**
	 * Loads a graph from a txt file.
	 * 
	 * @param fileName
	 * @return
	 */
	public static Grafo loadFromTxt(String fileName) {

		try {

			File inputFile = new File(fileName);
			RandomAccessFile in = new RandomAccessFile(inputFile, "r");

			String line;
			int lines = 1;
			int begVertex;
			int endVertex;
			boolean firstLine = true;

			Grafo g = null;
			while ((line = in.readLine()) != null) {
				if (!(line.startsWith("#")) && !line.equals("\n") && !line.equals("")) {
					if (firstLine) {												
						lines = Integer.parseInt(line.trim());
						firstLine = false;
						g = new Grafo(lines);

					} else {
						int commaIndex = line.indexOf(",");
						begVertex = Integer.parseInt(line.substring(0, commaIndex).trim());
						endVertex = Integer.parseInt(line.substring(commaIndex + 1).trim());

						g.setArista(begVertex, endVertex);
					}
				}

			}
			in.close();

			return g;

		} catch (Exception e) {
			System.out.println("Error en: GraphUtils.loadFromTxt()");
			System.out.println("Archivo de entrada erróneo.");
			e.printStackTrace(System.out);

			return null;
		}
	}
	
	
	
}
