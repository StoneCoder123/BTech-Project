package in.ud.convert;

import java.util.HashMap;
import java.util.Map;

public class POSMapper {
    private static final Map<String, String> POS_MAP = new HashMap<>();

    static {
        POS_MAP.put("NN", "NOUN");
        POS_MAP.put("N_NN", "NOUN");
        POS_MAP.put("NNP", "PROPN");
        POS_MAP.put("N_NNP", "PROPN");
        POS_MAP.put("PRP", "PRON");
        POS_MAP.put("PR_PRP", "PRON");
        POS_MAP.put("VM", "VERB");
        POS_MAP.put("V_VM", "VERB");
        POS_MAP.put("VAUX", "AUX");
        POS_MAP.put("V_VAUX", "AUX");
        POS_MAP.put("JJ", "ADJ");
        POS_MAP.put("RB", "ADV");
        POS_MAP.put("PSP", "ADP");
        POS_MAP.put("CC_CCD", "CCONJ");
        POS_MAP.put("CC_CCS", "SCONJ");
        POS_MAP.put("QT_QTC", "NUM");
        POS_MAP.put("QT_QTF", "NUM");
        POS_MAP.put("QT_QTO", "NUM");
        POS_MAP.put("RD_PUNC", "PUNCT");
        POS_MAP.put("RD_SYM", "SYM");
        POS_MAP.put("RD_RDF", "X");
        POS_MAP.put("DM_DMD", "DET");
        // Add more mappings as needed
    }

    public String map(String xpos) {
        return POS_MAP.getOrDefault(xpos, "X");
    }
}
