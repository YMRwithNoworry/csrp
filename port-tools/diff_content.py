import os, re

O = r"D:/code/MC模组/_srp-orig/jar/assets/srparasites"
P = r"D:/code/MC模组/csrp-1.20.1-forge/src/main/resources/assets/csrp"

def names(root, sub, ext='.json'):
    d = os.path.join(root, sub)
    if not os.path.isdir(d): return set()
    return {f[:-len(ext)] for f in os.listdir(d) if f.endswith(ext)}

for sub,label in [('models/item','ITEMS (models/item)'), ('blockstates','BLOCKS (blockstates)')]:
    o = names(O, sub); p = names(P, sub)
    miss = sorted(o-p); extra = sorted(p-o)
    print(f"\n===== {label} =====")
    print(f"orig={len(o)}  port={len(p)}  missing={len(miss)}  extra={len(extra)}")
    if miss:
        print("--- missing:", ", ".join(miss[:200]))
    if extra:
        print("--- extra:", ", ".join(extra[:200]))
