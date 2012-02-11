package ca.utoronto.msrg.padres.broker.router.matching;

public class DuplicateMsgFoundException extends Exception {

	private static final long serialVersionUID = 8309711156837836940L;

	public DuplicateMsgFoundException() {
		super();
	}

	public DuplicateMsgFoundException(String msg) {
		super(msg);
	}
}
