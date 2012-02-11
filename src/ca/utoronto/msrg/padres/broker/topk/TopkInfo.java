package ca.utoronto.msrg.padres.broker.topk;

public class TopkInfo {

	private int k, W, shift, chunk;
	
	public TopkInfo(int k, int W, int shift, int chunk) {
		this.k = k;
		this.W = W;
		this.shift = shift;
		this.chunk = chunk;
	}

	public String toString(){
		return "k: " + k + ", W: " + W + ", shift: " + shift + ", chunk: " + chunk;
	}

	public int getTopKSize() {
		return k;
	}

	public int getWindowSize() {
		return W;
	}

	public int getShift() {
		return shift;
	}
	
	public int getChunkSize() {
		return chunk;
	}
}
