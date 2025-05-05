package in.ud.convert;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SSFtoCoNLLUConverter {

    private final POSMapper posMapper = new POSMapper();

    static class Token {
        int id;
        String form;
        String lemma;
        String upos;
        String xpos;
        String feats;
        int head; // ID of head token, 0 for root
        String deprel;
        String deps = "_";
        String misc = "_";
        String chunkName; // For dependency mapping
        String drel; // e.g., k1:VGF
        String drelLabel; // e.g., k1
        String drelParentChunk; // e.g., VGF

        Token(int id, String form, String lemma, String upos, String xpos, String feats,
                String chunkName, String drel, String drelLabel, String drelParentChunk) {
            this.id = id;
            this.form = form;
            this.lemma = lemma;
            this.upos = upos;
            this.xpos = xpos;
            this.feats = feats;
            this.chunkName = chunkName;
            this.drel = drel;
            this.drelLabel = drelLabel;
            this.drelParentChunk = drelParentChunk;
        }
    }

    static class Chunk {
        String name;
        String drel; // e.g., k1:VGF
        String drelLabel;
        String drelParentChunk;
        List<Token> tokens = new ArrayList<>();
        Token headToken; // The syntactic head of the chunk
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java in.ud.convert.SSFtoCoNLLUConverter <input.txt> <output.conllu>");
            System.exit(1);
        }
        new SSFtoCoNLLUConverter().convert(args[0], args[1]);
    }

    public void convert(String inputPath, String outputPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), "UTF-8"));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(outputPath), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<Sentence")) {
                    String sentId = extractAttr(line, "id");
                    List<Token> tokens = new ArrayList<>();
                    Map<String, Chunk> chunkMap = new HashMap<>();
                    Map<Integer, Token> idToToken = new HashMap<>();
                    Stack<Chunk> chunkStack = new Stack<>();
                    int tokenId = 1;

                    // Read sentence content
                    while ((line = reader.readLine()) != null && !line.trim().startsWith("</Sentence")) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;

                        // Start of a chunk
                        if (line.matches("\\d+\\s+\\(\\(\\s*\\w+\\s*<fs.*")) {
                            String[] parts = line.split("\\s+", 4);
                            String chunkName = parts[2];
                            String fs = parts[3];
                            String drel = extractAttr(fs, "drel");
                            String drelLabel = null, drelParent = null;
                            if (drel != null && drel.contains(":")) {
                                String[] drelParts = drel.split(":");
                                drelLabel = drelParts[0];
                                drelParent = drelParts[1];
                            }
                            Chunk chunk = new Chunk();
                            chunk.name = chunkName;
                            chunk.drel = drel;
                            chunk.drelLabel = drelLabel;
                            chunk.drelParentChunk = drelParent;
                            chunkStack.push(chunk);
                        }
                        // End of a chunk
                        else if (line.equals("))")) {
                            Chunk finished = chunkStack.pop();
                            // Heuristic: head = last token in chunk (common in Hindi/Indian treebanks)
                            if (!finished.tokens.isEmpty())
                                finished.headToken = finished.tokens.get(finished.tokens.size() - 1);
                            chunkMap.put(finished.name, finished);
                            if (!chunkStack.isEmpty()) {
                                chunkStack.peek().tokens.addAll(finished.tokens);
                            }
                        }
                        // Terminal token line
                        else if (line.matches("\\d+\\.\\d+\\s+.*")) {
                            String[] parts = line.split("\\t");
                            if (parts.length < 4)
                                continue;
                            String form = parts[1];
                            String xpos = parts[2];
                            String fs = parts[3];
                            String lemma = getLemma(fs, form);
                            String feats = parseFeatures(fs);
                            String chunkName = null, drel = null, drelLabel = null, drelParent = null;
                            Matcher chunkMatcher = Pattern.compile("name='([^']+)'").matcher(fs);
                            if (chunkMatcher.find())
                                chunkName = chunkMatcher.group(1);
                            drel = extractAttr(fs, "drel");
                            if (drel != null && drel.contains(":")) {
                                String[] drelParts = drel.split(":");
                                drelLabel = drelParts[0];
                                drelParent = drelParts[1];
                            }
                            Token token = new Token(tokenId, form, lemma, posMapper.map(xpos), xpos, feats, chunkName,
                                    drel, drelLabel, drelParent);
                            tokens.add(token);
                            idToToken.put(tokenId, token);
                            tokenId++;
                            if (!chunkStack.isEmpty())
                                chunkStack.peek().tokens.add(token);
                        }
                    }

                    // Map from chunk name to its head token
                    Map<String, Token> chunkHeadMap = new HashMap<>();
                    for (Chunk c : chunkMap.values()) {
                        if (c.headToken != null)
                            chunkHeadMap.put(c.name, c.headToken);
                    }

                    // Assign HEAD and DEPREL for each token (inter-chunk)
                    for (Token t : tokens) {
                        if (t.drelParentChunk != null && chunkHeadMap.containsKey(t.drelParentChunk)) {
                            Token headTok = chunkHeadMap.get(t.drelParentChunk);
                            t.head = headTok.id;
                            t.deprel = t.drelLabel != null ? t.drelLabel : "dep";
                        } else {
                            t.head = 0; // root
                            t.deprel = "root";
                        }
                    }

                    // Output sentence-level comment and tokens in CoNLL-U
                    writer.write("# sent_id = " + sentId + "\n");
                    for (Token t : tokens) {
                        writer.write(String.format("%d\t%s\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\n",
                                t.id, t.form, t.lemma, t.upos, t.xpos, t.feats, t.head, t.deprel, t.deps, t.misc));
                    }
                    writer.write("\n");
                }
            }
        }
    }

    private String getLemma(String fs, String fallback) {
        int afIndex = fs.indexOf("af='");
        if (afIndex != -1) {
            int end = fs.indexOf("'", afIndex + 4);
            if (end != -1) {
                String af = fs.substring(afIndex + 4, end);
                String[] attrs = af.split(",");
                if (attrs.length > 0 && !attrs[0].isEmpty()) {
                    return attrs[0];
                }
            }
        }
        return fallback;
    }

    private String parseFeatures(String fs) {
        int afIndex = fs.indexOf("af='");
        if (afIndex != -1) {
            int end = fs.indexOf("'", afIndex + 4);
            if (end != -1) {
                String af = fs.substring(afIndex + 4, end);
                String[] attrs = af.split(",");
                List<String> feats = new ArrayList<>();
                if (attrs.length >= 3 && !attrs[2].isEmpty())
                    feats.add("Gender=" + mapGender(attrs[2]));
                if (attrs.length >= 4 && !attrs[3].isEmpty())
                    feats.add("Number=" + mapNumber(attrs[3]));
                if (attrs.length >= 5 && !attrs[4].isEmpty())
                    feats.add("Person=" + attrs[4]);
                // Add more mappings as needed
                return feats.isEmpty() ? "_" : String.join("|", feats);
            }
        }
        return "_";
    }

    private String mapGender(String g) {
        switch (g) {
            case "m":
                return "Masc";
            case "f":
                return "Fem";
            case "any":
                return "Any";
            default:
                return g;
        }
    }

    private String mapNumber(String n) {
        switch (n) {
            case "sg":
                return "Sing";
            case "pl":
                return "Plur";
            case "any":
                return "Any";
            default:
                return n;
        }
    }

    // Helper method to extract attribute value from feature string
    private static String extractAttr(String fs, String attr) {
        Pattern p = Pattern.compile(attr + "=\\'([^\\']+)\\'");
        Matcher m = p.matcher(fs);
        if (m.find())
            return m.group(1);
        return null;
    }
}
