import re, os

ORIG = r"D:/code/模组反编译器/decompiled/[逃逸：寄生体] SRParasites-1.10.8/com/dhanantry/scapeandrunparasites"
PORT = r"D:/code/MC模组/csrp-1.20.1-forge/src/main/java/alku/csrp"

orig_src = open(os.path.join(ORIG, "init/SRPEntities.java"), encoding='utf-8', errors='replace').read()
pat = re.compile(r'CreateEntity(?:Mob|Projectile|NoEgg)\("([^"]+)",\s*([A-Za-z0-9_]+)\.class')
orig_entities = {m.group(1): m.group(2) for m in pat.finditer(orig_src)}

port_src = open(os.path.join(PORT, "registry/ModEntities.java"), encoding='utf-8', errors='replace').read()
port_names = set()
for m in re.finditer(r'ENTITIES\.register\(\s*"([a-z0-9_]+)"', port_src): port_names.add(m.group(1))
for m in re.finditer(r'\bmonster\(\s*"([a-z0-9_]+)"', port_src): port_names.add(m.group(1))
for m in re.finditer(r'\bprojectile\(\s*"([a-z0-9_]+)"', port_src): port_names.add(m.group(1))
print("orig registered entities:", len(orig_entities))
print("port registered entities:", len(port_names))

missing = sorted(set(orig_entities) - port_names)
extra = sorted(port_names - set(orig_entities))
print("\n=== MISSING IN PORT (%d) ===" % len(missing))
for k in missing:
    print(f"  {k:28s} <- {orig_entities[k]}")
print("\n=== EXTRA / renamed IN PORT (%d) ===" % len(extra))
for k in extra:
    print(f"  {k}")
