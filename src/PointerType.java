class PointerType extends CompositeType {
	public Type nextType; //ptr to the next ptr Type
	PointerType() {
		super("pointer", 4);
	}

	public String getName() {
		String name = getBase().getName();

		if(name == null) {
			return "pointer";
		}

		Type header = this;
		while((header instanceof PointerType) && ((PointerType) header).getNext() != null) {
			name = name + "*";
			header = ((PointerType) header).getNext();
		}
		return name;
	}

	public void setNext(Type typ) {
		nextType = typ;
	}

	public Type getNext() {
		if(nextType != null ) {
			return nextType;
		} else {
			return null;
		}
	}

	public boolean isPointer() { return true; }

	public boolean isEquivalentTo(Type typ) {
		 if(!(typ instanceof PointerType)) {
		 	return false;
		 }

		 if(!(this.getBase().getClass().equals(((PointerType) typ).getBase().getClass()))) {
		 	return false;
		 }

		 if(!(getName().equals(typ.getName()))) return false;
		 
		 return true;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}

	public Type addLast(Type newNode) {
		Type header = this;
		Type ret = header; //The beggining of the array chain 

		if(header == null) {
			return new PointerType();
		}
		while(((PointerType) header).getNext() != null) {
			if(header instanceof PointerType) {
				header = ((PointerType) header).getNext();
			}
		}
		if(header instanceof PointerType) {
			((PointerType) header).setNext(newNode);
		}

		return ret;
	}

	public boolean hasErrorType() {
		Type baseType = this;

		while((baseType instanceof PointerType) && ((PointerType) baseType).getNext() != null) {
			if(baseType instanceof PointerType) {
				baseType = ((PointerType) baseType).getNext();
			}
		}

		if(baseType instanceof ErrorType) {
			return true;
		}

		return false;
	}

	public Type getBase() {
		Type header = this;
		if(header == null) {
			return new PointerType();
		}

		while((header instanceof PointerType) && ((PointerType) header).getNext() != null) {
			header = ((PointerType) header).getNext();
		}
		return header; 
	}
}