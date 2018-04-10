class IntType extends NumericType {
	public IntType() {
		super("int");
	}
	public boolean isInt() { return true; }

	public boolean isEquivalentTo(Type typ) {
		return (typ.getName() == "int") ? true : false;
	}

	public boolean isAssignableTo(Type typ) {
		return (isEquivalentTo(typ) || typ.isFloat());
	}
}