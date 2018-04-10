import java.math.BigDecimal;
class ModOp extends ArithmeticOp {
	public ModOp(String _op){
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

			if(((ConstSTO) b).getIntValue() == 0) {
				return new ErrorSTO(ErrorMsg.error8_Arithmetic);
			}

			BigDecimal aVal = ((ConstSTO) a).getValue();
		 	BigDecimal bVal = ((ConstSTO) b).getValue();
		 	BigDecimal result = aVal.remainder(bVal);
		 	return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new IntType(), result);
		}
		return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new IntType());
	}
}