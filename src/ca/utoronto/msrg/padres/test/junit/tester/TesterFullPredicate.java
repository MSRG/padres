package ca.utoronto.msrg.padres.test.junit.tester;

import java.io.Serializable;
import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Predicate;

/**
 * Auxiliary class used as part of test framework. The calss implements
 * an object that holds attribute/operator/value tuples.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterFullPredicate {
	public final String _attr;
	public final String _op;
	public final Serializable _val;
	
	TesterFullPredicate(String attr, String op, Serializable val) {
		if(attr == null)
			throw new NullPointerException();
		
		_attr = attr;
		_op = op;
		_val = val;
	}
	
	public boolean predicateExists(Map<String, Predicate> msgPredicatesMap) {
		Predicate pred = msgPredicatesMap.get(_attr);
		if(pred == null)
			return false;
		
		if(_op != null) {
			String op = pred.getOp();
			if(op == null || !_op.equals(op))
				return false;
		}
		
		if(_val != null) {
			Object val = pred.getValue();
			if(val == null || _val.equals(val))
					return false;
		}
		
		return true;
	}
	
	public boolean predicateExists(TesterMessagePredicates msgPredicates) {
		for(TesterFullPredicate pred : msgPredicates._list)
			if(_attr == null || _attr.equals(pred._attr))
				if(_op == null || _op.equals(pred._op))
					if(_val == null || _val.equals(pred._val))
						return true;
		
		return false;
	}
	
	@Override
	public String toString() {
		return "(" + _attr + "," + _op + "," + _val + ")";
	}
}