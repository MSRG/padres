package ca.utoronto.msrg.padres.test.junit.tester;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

/**
 * Auxiliary class used as part of test framework. The class implements 
 * a generic interface to match message predicates registered as part of
 * expected and unexpected events with the test framework.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterMessagePredicates {
	protected List<TesterFullPredicate> _list =
		new LinkedList<TesterFullPredicate>();
	
	public TesterMessagePredicates addPredicate(String attr,
			String op, Serializable val) {
		addPredicate(new TesterFullPredicate(attr, op, val));
		return this;
	}
	
	public TesterMessagePredicates addPredicate(TesterFullPredicate pred) {
		_list.add(pred);
		return this;
	}
	
	public TesterMessagePredicates clearAll() {
		_list.clear();
		return this;
	}
	
	public static TesterMessagePredicates createTesterMessagePredicates(Advertisement adv) {
		TesterMessagePredicates ret = new TesterMessagePredicates();
		for(Entry<String, Predicate> pair : adv.getPredicateMap().entrySet()) {
			Predicate pred = pair.getValue();
			ret.addPredicate(pair.getKey(), pred.getOp(), (Serializable)pred.getValue());
		}
		
		return ret;
	}
	
	public static TesterMessagePredicates createTesterMessagePredicates(Subscription sub) {
		TesterMessagePredicates ret = new TesterMessagePredicates();
		for(Entry<String, Predicate> pair : sub.getPredicateMap().entrySet()) {
			Predicate pred = pair.getValue();
			ret.addPredicate(pair.getKey(), pred.getOp(), (Serializable)pred.getValue());
		}
		
		return ret;
	}
	
	public static TesterMessagePredicates createTesterMessagePredicates(Publication pub) {
		TesterMessagePredicates ret = new TesterMessagePredicates();
		for(Entry<String, Serializable> pair : pub.getPairMap().entrySet()) {
			Serializable val = pair.getValue();
			if(val == null)
				continue;
			if(String.class.isAssignableFrom(val.getClass()))
				ret.addPredicate(pair.getKey(), "eq", val);
			else
				ret.addPredicate(pair.getKey(), "=", val);
		}
		
		return ret;
	}
	
	public boolean match(Map<String, Predicate> preds) {
		for(TesterFullPredicate pred : _list)
			if(!pred.predicateExists(preds))
				return false;
		
		return true;
	}
	
	public boolean match(TesterMessagePredicates msgPredicates) {
		for(TesterFullPredicate pred : _list)
			if(!pred.predicateExists(msgPredicates))
				return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		writer.append("Predicates={");
		writer.append(_list.toString());
		writer.append("}");
		return writer.toString();
	}
}