class ArrayType extends CompositeType {
	public Type nextType; //ptr to the next array in multi-dim array
	public int aryLen; //number of elements in array

	ArrayType(int len) {
		super("array");
		setLen(len);
		//*NOTE: Must set baseType and size after CTor call
	}

	public void setLen(int len) {
		aryLen = len;
	}

	public int getLen() {
		return aryLen;
	}

	public String getName() {
		if(nextType == null) {
			return "array";
		}

		String name = getBase().getName();

		Type header = this;
		while((header instanceof ArrayType) && ((ArrayType) header).getNext() != null) {
			name = name + "[" + ((ArrayType) header).getLen() + "]";
			header = ((ArrayType) header).getNext();
		}
		return name;
		  
	}

	public void setArraySize() {
		setSize(getBase().getSize()*getLen());
	}

	public int getSize() {
		return (getBase().getSize())*getLen();
	}

	public Type getBase() {
		Type baseType = this;

		while((baseType instanceof ArrayType) && ((ArrayType) baseType).getNext() != null) {
			if(baseType instanceof ArrayType) {
				baseType = ((ArrayType) baseType).getNext();
			}
		}
		return baseType;
	}

	public void setNext(Type typ) {
		nextType = typ;
	}

	public Type getNext() {
		return nextType;
	}

	public boolean isArray() { return true; }

	public boolean isEquivalentTo(Type typ) {
		return (typ.getName().equals(getName()))? true : false;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}

	public Type addLast(Type newNode) {
		Type header = this;
		Type ret = header; //The beggining of the array chain 

		if(header == null) {
			return new ArrayType(0);
		}
		while(((ArrayType) header).getNext() != null) {
			if(header instanceof ArrayType) {
				header = ((ArrayType) header).getNext();
			}
		}
		if(header instanceof ArrayType) {
			((ArrayType) header).setNext(newNode);
		}

		return ret;
	}

	public boolean hasErrorType() {
		Type baseType = this;

		while((baseType instanceof ArrayType) && ((ArrayType) baseType).getNext() != null) {
			if(baseType instanceof ArrayType) {
				baseType = ((ArrayType) baseType).getNext();
			}
		}

		if(baseType instanceof ErrorType) {
			return true;
		}

		return false;
	}

	public void printTypeChain(String msg) {
		System.out.println(msg);
		System.out.println("-----------------------");
		
		Type header = this;

		if(header == null) {
			return;
		}
		while((header instanceof ArrayType) && ((ArrayType) header).getNext() != null) {
				System.out.println("Type: " + header.getName());
				header = ((ArrayType) header).getNext();
		}
		System.out.println("Type: " + header.getName());
	}
}