package mbt.branch.and.price;

public class Offset {
	
	public final int vertex;	
	public final int offsetValue;
	
	public Offset(int vertex, int offsetValue) {
		this.vertex = vertex;
		this.offsetValue = offsetValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + offsetValue;
		result = prime * result + vertex;
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
		Offset other = (Offset) obj;
		if (offsetValue != other.offsetValue)
			return false;
		if (vertex != other.vertex)
			return false;
		return true;
	}
	

}
