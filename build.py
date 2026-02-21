#!/usr/bin/env python3

from subprocess import run
from sys import argv, exit
import os
import xml.etree.ElementTree as ET

RED    = "\033[31m"
YELLOW = "\033[33m"
RESET  = "\033[0m"
BOLD   = "\033[1m"

# --------- Utils ---------
def Todo(msg: str):
    print("TODO: " + msg)

def build(build_mode: str):
    build_command = [
        "./gradlew", build_mode
    ]

    return run(build_command).returncode

def open_apk(app_path: str):
    open_command = [
        "termux-open", app_path
    ]

    print(f"Openning {app_path}")
    run(open_command)

def load_env():
    if os.path.isfile(".env"):
        with open(".env") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" in line:
                    key, value = line.split("=", 1)
                    os.environ[key.strip()] = value.strip()

def require_env(var_name):
    value = os.environ.get(var_name)
    if not value:
        print(f"[!] {var_name} not set")
        exit(1)
    return value

def generate_keystore(path, storepass, keypass, alias):
    print("[!] Keystore not found, generating new one...")

    cmd = [
        "keytool",
        "-genkeypair",
        "-keystore", path,
        "-storepass", storepass,
        "-keypass", keypass,
        "-alias", alias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000"
    ]

    run(cmd, check=True)
    print(f"[+] Keystore created at {path}")

def lint(build_mode):
    xml_path = "app/lint-baseline.xml"
    if os.path.exists(xml_path):
        os.remove(xml_path)

    if build(build_mode) != 0:

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

def release(build_mode):
    keystore_path = require_env("KEYSTORE_PATH")
    keystore_password = require_env("KEYSTORE_PASSWORD")
    key_password = require_env("KEY_PASSWORD")
    key_alias = require_env("KEY_ALIAS")

    if not os.path.isfile(keystore_path):
        generate_keystore(
            keystore_path,
            keystore_password,
            key_password,
            key_alias
        )
    else:
        print("[+] Keystore already exists")

    if build(build_mode) == 0:
        app_path = "app/build/outputs/apk/release/app-release.apk"
        open_apk(app_path)

def debug(build_mode):
   if build(build_mode) == 0:
       app_path = "app/build/outputs/apk/debug/app-debug.apk"
       open_apk(app_path)

def main():

    build_mode = argv[1]
    final: str = build_mode.upper()

    if "DEBUG" in final:
        debug(build_mode)
        return 0

    elif "RELEASE" in final:
        load_env()
        release(build_mode)
        return 0

    elif "LINT" in final:
        lint(build_mode)
        return 0

    else:
        build(build_mode)

if __name__ == "__main__":
    exit(main())
