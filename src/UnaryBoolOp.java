class UnaryBoolOp extends UnaryOp {
	public UnaryBoolOp(String _op){
		super(_op);
	}
	public STO checkOperand(STO a) {
		if (a instanceof ErrorSTO) 
			return a;
		Type aType = a.getType();

		if (!(aType instanceof BoolType)) {
			setErrType(aType);
			setExpectedType(new BoolType());
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error1u_Expr, 
		 		getErrType().getName(), getName(), (new BoolType()).getName()));
		 }

		 if(a instanceof ConstSTO) {
		 	boolean aVal = ((ConstSTO) a).getBoolValue();
		 	int result = 0;
		 	switch(operator) {
				case "!":
					result = (!aVal) ? 1 : 0; //convert bool to int using ! operator
				break;
			}
		 	return new ConstSTO(operator + "(" + a.getName() + ")", new BoolType(), result);
		 }

		 // return ExprSTO of float type
		 return new ExprSTO(operator + "(" + a.getName() + ")", new BoolType());	 
	}
}