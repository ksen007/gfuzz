mvn clean package
mvn exec:java -Dexec.mainClass=ProcessTree -Dexec.args="src/main/examples/TObjectPrimitiveHashMapTest.java"
mvn exec:java -Dexec.mainClass=generator.ProgramGenerator -Dexec.args="src/main/examples/TObjectPrimitiveHashMapTest.java.tok"
