abstract class UnaryOp extends Operator {
	public UnaryOp(String _op){
		super(_op);
	}
	public abstract STO checkOperand(STO a);
}