import sys
import re

def extract_ssf_tokens(ssf_path):
    sentences = []
    with open(ssf_path, 'r', encoding='utf-8') as f:
        tokens = []
        for line in f:
            line = line.strip()
            if line.startswith('<Sentence'):
                tokens = []
            elif line.startswith('</Sentence>'):
                if tokens:
                    sentences.append(tokens)
            elif re.match(r'\d+\.\d+\s+', line):
                parts = line.split('\t')
                if len(parts) >= 2:
                    tokens.append(parts[1])
    return sentences

def extract_conllu_tokens(conllu_path):
    sentences = []
    with open(conllu_path, 'r', encoding='utf-8') as f:
        tokens = []
        for line in f:
            line = line.strip()
            if not line:
                if tokens:
                    sentences.append(tokens)
                    tokens = []
            elif not line.startswith('#'):
                parts = line.split('\t')
                if len(parts) == 10:
                    tokens.append(parts[1])
        if tokens:
            sentences.append(tokens)
    return sentences

def validate(ssf_file, conllu_file):
    ssf_sentences = extract_ssf_tokens(ssf_file)
    conllu_sentences = extract_conllu_tokens(conllu_file)

    if len(ssf_sentences) != len(conllu_sentences):
        print(f"Mismatch in number of sentences: SSF={len(ssf_sentences)} CoNLL-U={len(conllu_sentences)}")
        return False

    for idx, (ssf_toks, conllu_toks) in enumerate(zip(ssf_sentences, conllu_sentences), start=1):
        if ssf_toks != conllu_toks:
            print(f"Sentence {idx} tokens mismatch:")
            print(f"  SSF:    {' '.join(ssf_toks)}")
            print(f"  CoNLLU: {' '.join(conllu_toks)}")
            return False

    print("All sentences match between SSF and CoNLL-U.")
    return True

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python validate_ssf_conllu.py <input.ssf> <output.conllu>")
        sys.exit(1)

    ssf_path = sys.argv[1]
    conllu_path = sys.argv[2]
    valid = validate(ssf_path, conllu_path)
    sys.exit(0 if valid else 1)