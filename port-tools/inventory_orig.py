import os, re, collections, json, sys

ORIG = r"D:/code/模组反编译器/decompiled/[逃逸：寄生体] SRParasites-1.10.8/com/dhanantry/scapeandrunparasites"

def walk(d):
    out=[]
    for root,dirs,files in os.walk(d):
        for f in files:
            out.append(os.path.join(root,f))
    return out

files = walk(ORIG)
java = [f for f in files if f.endswith('.java')]
print("== total java files:", len(java))

rel = [os.path.relpath(f, ORIG).replace('\\','/') for f in java]
pkgs = collections.Counter(os.path.dirname(r) for r in rel)
print("\n== files per package (top 30) ==")
for k,v in pkgs.most_common(30):
    print(f"{v:5d}  {k}")

# entity monsters
mon = [r for r in rel if r.startswith('entity/monster/')]
print("\n== entity/monster:", len(mon))
for r in sorted(mon): print("   ", os.path.basename(r))

ai = [r for r in rel if r.startswith('entity/ai/')]
print("\n== entity/ai:", len(ai))
proj = [r for r in rel if r.startswith('entity/projectile/')]
print("\n== entity/projectile:", len(proj))
for r in sorted(proj): print("   ", os.path.basename(r))
