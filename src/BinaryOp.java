abstract class BinaryOp extends Operator {
	public BinaryOp(String _op){
		super(_op);
	};

	public abstract STO checkOperands(STO a, STO b);
}