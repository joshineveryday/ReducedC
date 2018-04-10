class UnarySignOp extends UnaryOp {
	public UnarySignOp(String _op){
		super(_op);
	}
	public STO checkOperand(STO a) {
		if (a instanceof ErrorSTO) 
			return a;
		Type aType = a.getType();

		if (!(aType instanceof NumericType)) {
			setErrType(aType);
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error1n_Expr, 
		 		aType.getName(), getName()));
		 }

		 if(a instanceof ConstSTO) {
		 	float aFloatVal = 0;
		 	int aIntVal = 0; 

		 	if(aType instanceof FloatType) {
				aFloatVal = ((ConstSTO) a).getFloatValue();
		 	} else {
		 		aIntVal = ((ConstSTO) a).getIntValue();
		 	}
		 	
		 	switch(operator) {
				case "-":
					if(aType instanceof FloatType) {
						aFloatVal = -1*aFloatVal; 
					} else {
						aIntVal = -1*aIntVal;
					}
				break;
				case "+":
					//do nothing
				break;
			}

			if(aType instanceof FloatType) {

				return new ConstSTO(operator + "(" + a.getName() + ")", new FloatType(), aFloatVal);
			} else {
				return new ConstSTO(operator + "(" + a.getName() + ")", new IntType(), aIntVal);
			}
		 	
		 }

		 return new ExprSTO(a.getName(), a.getType());
 
	}
}