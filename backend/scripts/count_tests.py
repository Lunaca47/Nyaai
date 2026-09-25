import glob
import xml.etree.ElementTree as ET

total = 0
for f in glob.glob("app/build/test-results/testDebugUnitTest/*.xml"):
    tree = ET.parse(f)
    root = tree.getroot()
    name = root.attrib.get("name", f)
    count = int(root.attrib.get("tests", 0))
    fail = int(root.attrib.get("failures", 0)) + int(root.attrib.get("errors", 0))
    print(f"{name}: {count} tests, {fail} failures")
    total += count
print(f"Total Android tests: {total}")
