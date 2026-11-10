#!/usr/bin/env bash
# ==============================================================================
# Fossify Gallery Cipher - Linux Quick Launcher
# ==============================================================================

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

# 1. Detect Desktop directory
if command -v xdg-user-dir >/dev/null 2>&1; then
    DESKTOP_DIR="$(xdg-user-dir DESKTOP)"
elif [ -d "$HOME/Desktop" ]; then
    DESKTOP_DIR="$HOME/Desktop"
elif [ -d "$HOME/Masaüstü" ]; then
    DESKTOP_DIR="$HOME/Masaüstü"
else
    DESKTOP_DIR="$HOME/Desktop"
fi

# 2. Check for existing folders or create default ones on Desktop
if [ -d "$DESKTOP_DIR/Sifrelenecek" ]; then
    INPUT_DIR="$DESKTOP_DIR/Sifrelenecek"
else
    INPUT_DIR="$DESKTOP_DIR/To_Encrypt"
fi

if [ -d "$DESKTOP_DIR/Sifreli_Cikti" ]; then
    OUTPUT_DIR="$DESKTOP_DIR/Sifreli_Cikti"
else
    OUTPUT_DIR="$DESKTOP_DIR/Encrypted_Output"
fi

mkdir -p "$INPUT_DIR" "$OUTPUT_DIR"
touch "$OUTPUT_DIR/.nomedia" 2>/dev/null || true

# 3. Check Python dependencies (cryptography, argon2, pillow)
python3 -c "import cryptography, argon2, PIL" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "=========================================================="
    echo "  [WARNING] Required Python libraries are missing!"
    echo "=========================================================="
    echo "To install on Debian-based Linux (e.g., Debian, Ubuntu, Tails), run in terminal:"
    echo ""
    echo "  sudo apt update && sudo apt install -y python3-cryptography python3-argon2 python3-pil"
    echo ""
    read -rp "Would you like to try installing them now via 'sudo apt'? [Y/n]: " DO_INSTALL
    if [[ "$DO_INSTALL" =~ ^([yY]|[yY][eE][sS]|[eE]|[eE][vV][eE][tT]|)$ ]]; then
        sudo apt update && sudo apt install -y python3-cryptography python3-argon2 python3-pil
    else
        echo "Cannot proceed without required cryptography libraries."
        exit 1
    fi
fi

# 4. Create double-clickable Desktop shortcut icon
DESKTOP_FILE="$DESKTOP_DIR/Fossify-Cipher.desktop"
if [ ! -f "$DESKTOP_FILE" ] && [ -d "$DESKTOP_DIR" ]; then
    cat <<EOF > "$DESKTOP_FILE"
[Desktop Entry]
Version=1.0
Type=Application
Name=Fossify Cipher Encryptor
Comment=Encrypt desktop photos for Android Fossify Gallery Cipher
Exec=sh -c 'cd "$DIR" && ./run.sh; echo; read -p "Press Enter to exit..."'
Icon=system-lock-screen
Terminal=true
Categories=Utility;Security;
EOF
    chmod +x "$DESKTOP_FILE" 2>/dev/null || true
    gio set "$DESKTOP_FILE" metadata::trusted true 2>/dev/null || true
fi

# 5. Launch CLI tool
python3 "$DIR/cipher_vault.py" "$@"
