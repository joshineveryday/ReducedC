import java.math.BigDecimal;
class BitwiseOp extends BinaryOp {
	public BitwiseOp(String _op ){
		super(_op);
	}
	public STO checkOperands(STO a, STO b) {
		if (a instanceof ErrorSTO) 
			return a;
		else if (b instanceof ErrorSTO)
			return b;
		Type aType = a.getType();
		Type bType = b.getType();
		if (!(aType instanceof IntType) || !(bType  instanceof IntType)) {
			if(!(aType instanceof IntType)) {
				setErrType(aType);
			} else {
				setErrType(bType);
			} 
			setExpectedType(new IntType());
			
		 	return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, 
		 		getErrType().getName(), getName(),(new IntType()).getName()));
		 } 

		 if(a instanceof ConstSTO && b instanceof ConstSTO) {
		 	int aVal = ((ConstSTO) a).getIntValue();
		 	int bVal = ((ConstSTO) b).getIntValue();
		 	int result = 0; 
		 	switch(operator) {
		 		case "&":
		 			result = aVal & bVal;
		 		break;
		 		case "^":
		 			result = aVal ^ bVal;
		 		break;
		 		case "|":
		 			result = aVal | bVal;
		 		break;
		 	}
		 	return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new IntType(), result);
		 }
		 
		return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new IntType());
	}
}