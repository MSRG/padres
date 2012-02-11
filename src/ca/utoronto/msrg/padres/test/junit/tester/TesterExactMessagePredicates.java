package ca.utoronto.msrg.padres.test.junit.tester;

import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Predicate;

/**
 * Auxiliary class used as part of test framework. The class implements 
 * a generic interface to match message predicates registered as part of
 * expected and unexpected events with the test framework. Matching semantic
 * is similar to that of its super class (TesterMessagePredicates) except the
 * number of predicates must be equal in matched items.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterExactMessagePredicates extends TesterMessagePredicates {
	
	@Override
	public boolean match(TesterMessagePredicates msgPredicates) {
		if(_list.size() != msgPredicates._list.size())
			return false;
		
		else
			return super.match(msgPredicates);
	}
	
	@Override
	public boolean match(Map<String, Predicate> preds) {
		if(_list.size() != preds.size())
			return false;
		
		else
			return super.match(preds);
	}
}
