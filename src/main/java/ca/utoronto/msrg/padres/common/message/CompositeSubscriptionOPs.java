package ca.utoronto.msrg.padres.common.message;

public interface CompositeSubscriptionOPs {
	
	//OPs
	public static final String COMPOSIT_SUBSCRIPTION_AND = "&";
	public static final String COMPOSIT_SUBSCRIPTION_OR = "||";
	public static final String COMPOSIT_SUBSCRIPTION_LEFT_PARENTHSIS = "{";
	public static final String COMPOSIT_SUBSCRIPTION_RIGHT_PARENTHSIS = "}";
	public static final String COMPOSIT_SUBSCRIPTION_VARIBLES = "$";
	
	//Names
	public static final String COMPOSIT_SUBSCRIPTION_PRE_NAME = "@s";
	
	//Classes
	public static final String VARIBLE_CLASS_BYTE = "B";
	public static final String VARIBLE_CLASS_SHORT = "SH";
	public static final String VARIBLE_CLASS_INTEGER = "I";
	public static final String VARIBLE_CLASS_LONG = "L";
	public static final String VARIBLE_CLASS_FLOAT = "F";
	public static final String VARIBLE_CLASS_DOUBLE = "DO";
	public static final String VARIBLE_CLASS_DATE = "DA";
	public static final String VARIBLE_CLASS_STRING = "ST";
}