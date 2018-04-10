#---------------------------------------------------------------------
# CSE 131 Reduced-C Compiler Project
# Copyright (C) 2008-2015 Garo Bournoutian and Rick Ord
# University of California, San Diego
#---------------------------------------------------------------------

JAVACUP=$(PUBLIC)/Tools/java-cup-v11a.jar

SOURCES:sh = find src -type f -name '*.java' ! -name 'RC.java'

bin/%.class : src/%.java
	@mkdir -p bin
	javac -classpath $(JAVACUP) -d bin -sourcepath src $<

#First target is the default target if "make" is run without a target
new:
	make clean
	make rc

rc: src/parser.java bin/RC.class $(SOURCES:src/%.java=bin/%.class)
	chmod +x RC

src/parser.java: src/rc.cup
	cd src; java -jar $(JAVACUP) < rc.cup

clean:
	cd src; rm -f *.class parser.java sym.java *.o
	rm -f rc.s a.out
	rm -rf bin/

CC=cc
compile:
	$(CC) rc.s $(PUBLIC)/input.c $(PUBLIC)/output.s $(LINKOBJ)
