class BoolType extends BasicType {
	public BoolType() {
		super("bool");
	}
	public boolean isBool() { return true; }

	public boolean isEquivalentTo(Type typ) {
		return (typ.getName() == "bool") ? true : false;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}
}