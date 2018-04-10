import java.util.Vector;
class StructType extends CompositeType {
	public Scope scope;
	public int offsetAddr;
	

	StructType() {
		super("struct", -1);
		setScope(new Scope());
	}

	StructType(String id) {
		super(id, -1);
		setScope(new Scope());
	}

	public boolean isEquivalentTo(Type typ) {
		return ((typ.getName() == getName()))? true : false;
	}

	public boolean isAssignableTo(Type typ) {
		return isEquivalentTo(typ);
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public void setScope(Scope _scope) {
		scope = _scope;
	}
	
	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public Scope getScope() {
		return scope;
	}

	//----------------------------------------------------------------
	//
	//----------------------------------------------------------------
	public int getSize() {
		Vector<STO> scopeVec = scope.getScopeVec();
		int size = 0;

		for(STO sto : scopeVec) {
			if(sto instanceof FuncSTO) continue;
			size = size + sto.getType().getSize(); 
		}

		return size;
	}
}