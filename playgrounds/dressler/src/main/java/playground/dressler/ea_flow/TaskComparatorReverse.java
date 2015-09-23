package playground.dressler.ea_flow;

import java.util.Comparator;

// Comparator needs total order!!!
/**
 * Compares according to the right end of the interval (if the task has one), not just task.time
 */
class TaskComparatorReverse implements TaskComparatorI {
	private int offset;
	
	public TaskComparatorReverse(int offset) {
		this.offset = offset;
	}
	
	public int compare(BFTask first, BFTask second) {	
		if (first.ival != null && second.ival != null) {
			if (first.ival.getHighBound() > second.ival.getHighBound()) return -1;
		}	
		
		// reversed compared to forward comparator!
		
		if (first.time > second.time) return -1; 
		if (first.time < second.time) return 1;

		// sources before normal nodes before sinks
		// reversed compared to forward comparator!
		if (first.node.priority() > second.node.priority()) return -1;
		if (first.node.priority() < second.node.priority()) return 1;
		

		// Important! PriorityQueue assumes that compare = 0 implies the same object ...
		// The following is just to make that work ...
		
		// the same as in the forward comparator, although slightly redundant
		if (first.ival != null || second.ival != null) {
			if (first.ival == null && second.ival != null) return -1;
			if (first.ival != null && second.ival == null) return 1;

			if (first.ival.getLowBound() < second.ival.getLowBound()) return -1;
			if (first.ival.getLowBound() > second.ival.getLowBound()) return 1;

			if (first.ival.getHighBound() < second.ival.getHighBound()) return -1;
			if (first.ival.getHighBound() > second.ival.getHighBound()) return 1;
			
			/*if (first.ival.isScanned() && !second.ival.isScanned()) return -1;
			if (!first.ival.isScanned() && second.ival.isScanned()) return 1;
			
			if (first.ival.getReachable() && !second.ival.getReachable()) return -1;
			if (!first.ival.getReachable() && second.ival.getReachable()) return 1;*/
		}
		
		// false < true
		if (!first.reverse && second.reverse) return -1;
		if (first.reverse && !second.reverse) return 1;

		return first.node.getRealNode().getId().compareTo(second.node.getRealNode().getId());

	}

	@Override
	public int getValue(BFTask task) {		
		return offset - task.time;
	}
}