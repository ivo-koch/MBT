package mbt.branch.and.price;

public class BranchingStep {
		
    
    public final int offset;
    
    public final int verticeDestino;
    
    public final int verticeOrigenDeV0;
    
    
    public BranchingStep(int offset, int verticeOrigenDeV0, int verticeDestino) {
    	this.offset = offset;
    	this.verticeOrigenDeV0 = verticeOrigenDeV0;
    	this.verticeDestino = verticeDestino;
    }


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + offset;
		result = prime * result + verticeDestino;
		result = prime * result + verticeOrigenDeV0;
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
		BranchingStep other = (BranchingStep) obj;
		if (offset != other.offset)
			return false;
		if (verticeDestino != other.verticeDestino)
			return false;
		if (verticeOrigenDeV0 != other.verticeOrigenDeV0)
			return false;
		return true;
	}

}
