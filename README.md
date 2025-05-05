# SSF to CoNLLU Converter

This Java tool converts CPG Treebank SSF files to CoNLL-U format.

## Usage

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="in.ud.convert.SSFtoCoNLLUConverter" -Dexec.args="<input.txt> <output.txt>"
```

