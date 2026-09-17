import os, collections
ORIG_JAR = r"D:/code/MC模组/_srp-orig/jar/assets/srparasites"
PORT = r"D:/code/MC模组/csrp-1.20.1-forge/src/main/resources/assets/csrp"

def relset(root):
    out=set()
    for r,d,f in os.walk(root):
        for x in f:
            out.add(os.path.relpath(os.path.join(r,x), root).replace('\\','/'))
    return out

o = relset(ORIG_JAR)
p = relset(PORT)
print("orig asset files:", len(o), " port asset files:", len(p))

def bucket(s):
    c=collections.Counter()
    for f in s:
        parts=f.split('/')
        c[parts[0] if len(parts)>0 else ''] += 1
    return c

print("\n== orig top-level counts =="); [print(f"  {v:6d} {k}") for k,v in sorted(bucket(o).items(), key=lambda x:-x[1])]
print("\n== port top-level counts =="); [print(f"  {v:6d} {k}") for k,v in sorted(bucket(p).items(), key=lambda x:-x[1])]

missing = sorted(o - p)
print("\n== MISSING in port (%d) ==" % len(missing))
mc = collections.Counter('/'.join(f.split('/')[:2]) for f in missing)
for k,v in mc.most_common(40): print(f"  {v:6d} {k}")
