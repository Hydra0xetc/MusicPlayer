#!/usr/bin/env bash
set -e

if [ -f ".env" ]; then
    set -a
    source ./.env
    set +a
fi

IS_RELEASE=false
for arg in "$@"; do
    if [[ "$arg" == *"Release"* ]]; then
        IS_RELEASE=true
        break
    fi
done

if $IS_RELEASE; then
    echo "[*] Release build detected"

    : "${KEYSTORE_PATH:?KEYSTORE_PATH not set}"
    : "${KEYSTORE_PASSWORD:?KEYSTORE_PASSWORD not set}"
    : "${KEY_PASSWORD:?KEY_PASSWORD not set}"
    : "${KEY_ALIAS:?KEY_ALIAS not set}"

    if [ ! -f "$KEYSTORE_PATH" ]; then
        echo "[!] Keystore not found, generating new one..."

        keytool -genkeypair \
            -keystore "$KEYSTORE_PATH" \
            -storepass "$KEYSTORE_PASSWORD" \
            -keypass "$KEY_PASSWORD" \
            -alias "$KEY_ALIAS" \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000

        echo "[+] Keystore created at $KEYSTORE_PATH"
    else
        echo "[+] Keystore already exists"
    fi
fi

./gradlew "$@"

if $IS_RELEASE; then
    xdg-open ./app/build/outputs/apk/release/app-release.apk
    exit 0
fi

xdg-open app/build/outputs/apk/debug/app-debug.apk
