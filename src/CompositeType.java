class CompositeType extends Type {
	CompositeType(String name, int size) {
		super(name, size);
	}

	CompositeType(String name) {
		super(name, -1);
	}
}