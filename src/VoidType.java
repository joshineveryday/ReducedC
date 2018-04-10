class VoidType extends Type {
	public VoidType() {
		super("void", 0);
	}

	public boolean isVoid() { return true; }

	public boolean isEquivalentTo(Type typ) {
		return (typ.getName() == "void") ? true : false;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}
}