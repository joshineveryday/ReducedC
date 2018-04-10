 class EqOp extends ComparisonOp {
	public EqOp(String _op){
		super(_op);
	}
	public STO checkOperands(STO a, STO b) {
		if (a instanceof ErrorSTO) 
			return a;
		else if (b instanceof ErrorSTO)
			return b;
		Type aType = a.getType();
		Type bType = b.getType();


	 	 if((aType instanceof PointerType) || (bType instanceof PointerType)) {

	 	 		if((aType instanceof PointerType) ^ (bType instanceof PointerType)) {
					return new ErrorSTO(Formatter.toString(ErrorMsg.error17_Expr, 
		 							getName(), aType.getName(), bType.getName()));
	 	 		}

	 	 		Type aBase = ((PointerType) aType).getBase();
	 	 		Type bBase = ((PointerType) bType).getBase();

	 	 		if(!(aType.isEquivalentTo(bType)) && !(bType instanceof NullPointerType) && !(aType instanceof NullPointerType)) {
 	 				return new ErrorSTO(Formatter.toString(ErrorMsg.error17_Expr, 
		 							getName(), aType.getName(), bType.getName()));
	 	 		}
	 	 }  

	 	 if (!(aType instanceof NumericType && bType  instanceof NumericType) && 
			!(aType instanceof BoolType && bType  instanceof BoolType) &&
			!(aType instanceof PointerType && bType instanceof PointerType)

			) 
			{
				
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error1b_Expr, 
		 							aType.getName(), getName(), bType.getName()));
		 }
		 
		 if(a instanceof ConstSTO && b instanceof ConstSTO) {
			float aVal = ((ConstSTO) a).getFloatValue();
			float bVal = ((ConstSTO) b).getFloatValue();
			boolean bool_result = false; 
			switch(operator) {
				case "==":
					bool_result = (aVal == bVal);
				break;
				case "!=":
					bool_result = (aVal != bVal);
				break;
			}
			int result = (bool_result) ? 1 : 0; //convert bool to int
			return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType(), result);
		}



		 return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType());	 
	}
}