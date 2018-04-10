class FloatType extends NumericType {
	public FloatType() {
		super("float");
	}
	public boolean isFloat() { return true; }

	public boolean isEquivalentTo(Type typ) {
		if(typ != null) {
			return (typ.getName() == "float") ? true : false;
		} 
		return false;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}
}