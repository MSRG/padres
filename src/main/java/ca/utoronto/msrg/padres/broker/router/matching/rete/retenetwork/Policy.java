package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

public interface Policy {

	public boolean consumeEvent(NodeJoinUnify node, String otherMem, MemoryUnit mem, int matchCount);

	// boolean consumeEvent(NodeJoin n, String otherMem, Object p, int matchCount);
}
