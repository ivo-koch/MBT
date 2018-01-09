package mbt.branch.and.price.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import mbt.branch.and.price.Arbol;

public class ArbolTest {

	@Test
	public void testBFS() {

		Arbol.Builder builder = new Arbol.Builder(41, 8);

		// agregamos en cualquier orden
		builder.addVertex(5, 8);
		builder.addVertex(4, 8);
		builder.addVertex(7, 8);
		builder.addVertex(9, 7);
		builder.addVertex(10, 5);
		builder.addVertex(6, 10);
		builder.addVertex(1, 5);
		builder.addVertex(3, 1);
		builder.addVertex(2, 5);
		builder.addVertex(0, 9);
		builder.addVertex(11, 0);
		builder.addVertex(40, 8);

		// esperamos los vértices en orden 8, 4, 5, 7,40, 1, 2, 10, 9, 3, 6, 0, 11
		Arbol arbol = builder.buildArbol();
		List<Integer> bfs = arbol.bfs(8);

		assertEquals(8l, (long) bfs.get(0));
		assertEquals(4l, (long) bfs.get(1));
		assertEquals(5l, (long) bfs.get(2));
		assertEquals(7l, (long) bfs.get(3));
		assertEquals(40l, (long) bfs.get(4));
		assertEquals(1l, (long) bfs.get(5));
		assertEquals(2l, (long) bfs.get(6));
		assertEquals(10l, (long) bfs.get(7));
		assertEquals(9l, (long) bfs.get(8));
		assertEquals(3l, (long) bfs.get(9));
		assertEquals(6l, (long) bfs.get(10));
		assertEquals(0l, (long) bfs.get(11));		
		assertEquals(11l, (long) bfs.get(12));

	}
	

	@Test
	public void testGetCosto() {

		//un árbol con 12 vértice como máximo
		Arbol.Builder builder = new Arbol.Builder(12, 5);

		builder.addVertex(6, 5);
		builder.addVertex(1, 5);
		builder.addVertex(3, 5);
		builder.addVertex(2, 1);
		builder.addVertex(4, 5);
		builder.addVertex(7, 4);
		builder.addVertex(9, 4);
		builder.addVertex(11, 9);
		
		//assertEquals(4l, builder.buildArbol().calcularCosto());
		
		
	}

	
	
}
