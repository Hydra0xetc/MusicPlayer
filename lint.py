#!/usr/bin/env python3
import subprocess
import sys
import xml.etree.ElementTree as ET
import os

RED    = "\033[31m"
YELLOW = "\033[33m"
RESET  = "\033[0m"
BOLD   = "\033[1m"

def parse_lint(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()

    cwd = os.path.join(os.getcwd(), "app")

    for issue in root.findall("issue"):
        message = issue.attrib.get("message", "")

        location = issue.find("location")
        if location is None:
            continue

        file   = location.attrib.get("file", "")
        line   = location.attrib.get("line", "")
        column = location.attrib.get("column", "")

        # File:line:col red + bold
        print(f"{BOLD}{RED}{cwd}/{file}:{line}:{column}{RESET}")
        print(message + "\n")

def main():
    xml_path = "app/lint-baseline.xml"

    if os.path.exists(xml_path):
        os.remove(xml_path)

    subprocess.run(["./build.sh", "lint"])
    parse_lint(xml_path)

if __name__ == "__main__":
    sys.exit(main())
