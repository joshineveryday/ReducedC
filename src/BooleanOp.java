import java.math.BigDecimal;
class BooleanOp extends BinaryOp {
	public BooleanOp(String _op){
		super(_op);
	}
	public STO checkOperands(STO a, STO b) {
		if (a instanceof ErrorSTO) 
			return a;
		else if (b instanceof ErrorSTO)
			return b;
		Type aType = a.getType();
		Type bType = b.getType();

		if (!(aType instanceof BoolType) || !(bType  instanceof BoolType)) {
			if(!(aType instanceof BoolType)) {
				setErrType(aType);
			} else {
				setErrType(bType);	
			} 
			setExpectedType(new BoolType());
			return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, getErrType().getName(), getName(),(new BoolType()).getName()));
		 }

		if(a instanceof ConstSTO && b instanceof ConstSTO) {
			boolean aVal = ((ConstSTO) a).getBoolValue();
			boolean bVal = ((ConstSTO) b).getBoolValue();
			boolean bool_result = false; 
			switch(operator) {
				case "||":
					bool_result = aVal || bVal;
				break;
				case "&&":
					bool_result = aVal && bVal;
				break;
			}
			int result = (bool_result) ? 1 : 0; //convert bool to int
			return new ConstSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType(), result);
		}

		// return ExprSTO of float type
		return new ExprSTO("(" + a.getName() + ")" + operator + "(" + b.getName() + ")", new BoolType());	 
	}
}