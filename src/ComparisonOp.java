class ComparisonOp extends BinaryOp {
	public ComparisonOp(String _op){
		super(_op);
	}
	public STO checkOperands(STO a, STO b) {
		if (a instanceof ErrorSTO) 
			return a;
		else if (b instanceof ErrorSTO)
			return b;
		Type aType = a.getType();
		Type bType = b.getType();

		if (!(aType instanceof NumericType) || !(bType  instanceof NumericType)) {
			if(!(aType instanceof NumericType)) {
				setErrType(aType);
			} else {
				setErrType(bType);
			} 
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error1n_Expr,
		 		getErrType().getName(), getName()));
		 }

		 if(a instanceof ConstSTO && b instanceof ConstSTO) {
			float aVal = ((ConstSTO) a).getFloatValue();
			float bVal = ((ConstSTO) b).getFloatValue();
			boolean bool_result = false; 
			switch(operator) {
				case "<":
					bool_result = (aVal < bVal);
				break;
				case "<=":
					bool_result = (aVal <= bVal);
				break;
				case ">":
					bool_result = (aVal > bVal);
				break;
				case ">=":
					bool_result = (aVal >= bVal);
				break;
			}
			int result = (bool_result) ? 1 : 0; //convert bool to int
			return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType(), result);
		}


		return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType());
	}
}