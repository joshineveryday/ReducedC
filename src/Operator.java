abstract class Operator {
	Type errType; //if there is an error, this will be set to the type that is in error
	Type expType; //sets the expected type this op should return
	String operator; 
	public Operator(String _op){
		setErrType(null);
		setExpectedType(null);
		setName(_op);
	}

	public Type getErrType() {
		return errType;
	}

	//TODO may want to move getExpectedType logic into just ModOp 
	//or some other part of the class hierarchy. Not everything needs it.
	public void setErrType(Type typ) {
		errType = typ;
	}

	public Type getExpectedType() {
	 	return expType;
	}

	public void setExpectedType(Type typ) { 
		expType = typ;
	}	

	public String getName() {
		return operator;
	}

	public void setName(String _op) {
		operator = _op;
	}
}