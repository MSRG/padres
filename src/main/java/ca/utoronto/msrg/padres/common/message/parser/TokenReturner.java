package ca.utoronto.msrg.padres.common.message.parser;

public class TokenReturner {

	enum ValueType {
		LONG,
		DOUBLE,
		STRING;
	};
	
Token attr;
Token op;
Token val;
ValueType valtype = ValueType.STRING;
}
