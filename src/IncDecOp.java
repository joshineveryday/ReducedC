class IncDecOp extends UnaryOp {
	public IncDecOp(String _op){
		super(_op);
	}

	public String getName() {
		return operator.substring(1);
	}

	public String getFullName() {
		return operator;
	}

	public STO checkOperand(STO a) {
		if (a instanceof ErrorSTO) 
			return a;
		Type aType = a.getType();

		if (!(aType instanceof NumericType) && !(aType instanceof PointerType)) {

		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error2_Type,
		 		aType.getName(), getName()));

		 } else if (!(a.isModLValue())) {
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error2_Lval, getName()));
		 }

		 String stoName = "";
		 if(operator.equals("b++") || operator.equals("a++")) {
		 	stoName = "(" + a.getName() + ")" + operator;
		 } else {
			stoName = operator + "(" + a.getName() + ")";
		 }

		 if(aType instanceof PointerType) {
		 	return new ExprSTO(stoName, aType);	
		 }

		 return new ExprSTO(stoName, aType);	 
	}
}