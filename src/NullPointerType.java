class NullPointerType extends PointerType {
	NullPointerType() {
		super();
		setSize(0);
	}

	public String getName() {
		return "nullptr";
	}

	public boolean isEquivalentTo(Type typ) {
		 if(!(typ instanceof PointerType)) return false;
		 return true;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}
}