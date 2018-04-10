import java.math.BigDecimal;
import java.math.RoundingMode;
class ArithmeticOp extends BinaryOp {
	public ArithmeticOp(String _op){
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

		 Type retType; 
		 if (aType instanceof IntType && bType instanceof IntType) {
		 	// return ExprSTO of int type
		 	retType = new IntType();
		 } else {
		 	// return ExprSTO of float type
		 	retType = new FloatType();
		 }

		 if(a instanceof ConstSTO && b instanceof ConstSTO) {
		 	BigDecimal aVal = ((ConstSTO) a).getValue();
		 	BigDecimal bVal = ((ConstSTO) b).getValue();
		 	BigDecimal result = new BigDecimal(0); 

		 	if(((ConstSTO) b).getIntValue() == 0 && operator.equals("/")) {
				return new ErrorSTO(ErrorMsg.error8_Arithmetic);
			}

		 	switch(operator) {
		 		case "+":
		 			result = aVal.add(bVal);
		 		break;
		 		case "-":
		 			result = aVal.subtract(bVal);
		 		break;
		 		case "*":
		 			result = aVal.multiply(bVal);
		 		break;
		 		case "/":
		 			result = aVal.divide(bVal, 8, RoundingMode.FLOOR);
		 		break;
		 	}
		 	return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", retType, result);

		 }
		 
		 return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", retType);
	}
}