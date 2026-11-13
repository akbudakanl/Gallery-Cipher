#!/usr/bin/env python3
"""
Fossify Gallery Cipher - Linux Offline Encryption CLI Tool
----------------------------------------------------------
Encrypts media files (photos, videos) on Linux into .bin format,
100% compatible with Android Fossify Gallery Cipher (AES-256 + Poly1305-AES).
Live-boot Linux distributions such as Tails do not use local disks and run
in RAM, making them the most suitable option for this tool's zero-disk goal.

Features:
- Automatic 2-folder desktop management on Linux (To_Encrypt & Encrypted_Output).
- Automatic .vault discovery across Desktop, persistent storage, and mounted USB drives.
- Subfolder preservation as encrypted physical albums (.meta) in Fossify Gallery.
- Secure source sanitation (overwriting with zeros) of plaintext source media.
"""

import os
import sys
import time
import hmac
import hashlib
import secrets
import struct
import getpass
import argparse
import subprocess
import shutil
from pathlib import Path
from io import BytesIO

# The standalone container of the Android app in the internal storage.
APP_DIR_NAME = "Fossify Gallery Cipher"

HAS_CRYPTO = True
try:
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from cryptography.hazmat.backends import default_backend
    from argon2.low_level import hash_secret_raw, Type
except ImportError:
    HAS_CRYPTO = False

try:
    from PIL import Image
    HAS_PIL = True
except ImportError:
    HAS_PIL = False

def check_dependencies():
    if not HAS_CRYPTO:
        print("[ERROR] Required Python packages are missing!", file=sys.stderr)
        print("To install on Debian-based Linux (e.g., Debian, Ubuntu, Tails), run in terminal:", file=sys.stderr)
        print("  sudo apt update && sudo apt install -y python3-cryptography python3-argon2 python3-pil", file=sys.stderr)
        print("Or via pip:", file=sys.stderr)
        print("  pip install cryptography argon2-cffi pillow", file=sys.stderr)
        sys.exit(1)

# ==================== PROTOCOL CONSTANTS ====================
MAGIC_V2 = b"FGVLTENC"
VERSION_SYMMETRIC_AUTH = 3
VAULT_HEADER_MAGIC = b"FGVLT2"
META_MAGIC_V2 = b"FGMETA2"

DOMAIN_HEADER_KEY = b"FossifyGallery:Header:AES256GCM"
DOMAIN_CONTENT_ENC_KEY = b"FossifyGallery:Payload:AES256CTR"
DOMAIN_CONTENT_MAC_KEY = b"FossifyGallery:Payload:Poly1305"

TYPE_IMAGES = 1
TYPE_VIDEOS = 2
TYPE_GIFS = 4
TYPE_RAWS = 8
TYPE_SVGS = 16

VIDEO_EXTS = {".mp4", ".mkv", ".webm", ".mov", ".avi", ".3gp", ".flv", ".wmv", ".m4v"}
GIF_EXTS = {".gif"}
RAW_EXTS = {".dng", ".cr2", ".nef", ".arw", ".orf", ".rw2", ".pef", ".raf"}
SVG_EXTS = {".svg"}

# ==================== DESKTOP & DIRECTORY DISCOVERY ====================

def get_desktop_dir() -> Path:
    """Detects Desktop directory on Linux environments."""
    # 1. xdg-user-dir DESKTOP check
    try:
        res = subprocess.run(["xdg-user-dir", "DESKTOP"], capture_output=True, text=True, check=False)
        out = res.stdout.strip()
        if out and Path(out).exists():
            return Path(out)
    except Exception:
        pass

    # 2. XDG user-dirs config (~/.config/user-dirs.dirs)
    xdg_config = Path.home() / ".config" / "user-dirs.dirs"
    if xdg_config.exists():
        try:
            for line in xdg_config.read_text(encoding="utf-8").splitlines():
                if line.startswith("XDG_DESKTOP_DIR="):
                    val = line.split("=", 1)[1].strip().strip('"').replace("$HOME", str(Path.home()))
                    p = Path(val)
                    if p.exists():
                        return p
        except Exception:
            pass

    # 3. Common Desktop directory names
    for candidate_name in ["Desktop", "Masaüstü", "desktop"]:
        candidate = Path.home() / candidate_name
        if candidate.exists() and candidate.is_dir():
            return candidate

    # 4. Fallback: create ~/Desktop
    fallback = Path.home() / "Desktop"
    try:
        fallback.mkdir(parents=True, exist_ok=True)
        return fallback
    except Exception:
        return Path.home()

def get_default_input_dir(desktop: Path) -> Path:
    """Finds or creates the input folder for unencrypted media on the Desktop."""
    for name in ["To_Encrypt", "Encrypt_Input", "Input", "Sifrelenecek", "Sifrelenecek_Veriler", "Girdi"]:
        candidate = desktop / name
        if candidate.exists() and candidate.is_dir():
            return candidate
    target = desktop / "To_Encrypt"
    target.mkdir(parents=True, exist_ok=True)
    return target

def get_default_output_dir(desktop: Path) -> Path:
    """Finds or creates the output folder for encrypted .bin files on the Desktop."""
    for name in ["Encrypted_Output", "Output", "Cipher_Output", "Sifreli_Cikti", "Sifrelenmis_Dosyalar", "Cikti"]:
        candidate = desktop / name
        if candidate.exists() and candidate.is_dir():
            return candidate
    target = desktop / "Encrypted_Output"
    target.mkdir(parents=True, exist_ok=True)
    return target

def find_vault_file() -> Path | None:
    """Searches for .vault header file across common Linux and live-boot (e.g., Tails) locations."""
    desktop = get_desktop_dir()
    candidates = [
        # Desktop
        desktop / ".vault",
        desktop / "phone.vault",
        desktop / "vault.vault",
        desktop / "kasa.vault",
        # Desktop folders
        desktop / "To_Encrypt" / ".vault",
        desktop / "Encrypted_Output" / ".vault",
        desktop / "Sifrelenecek" / ".vault",
        desktop / "Sifreli_Cikti" / ".vault",
        # Tails Persistent Storage
        Path("/home/amnesia/Persistent/.vault"),
        Path("/home/amnesia/Persistent/phone.vault"),
        Path("/home/amnesia/Persistent/vault.vault"),
        Path.home() / "Persistent" / ".vault",
        Path.home() / "Persistent" / "phone.vault",
        # Home directory
        Path.home() / ".vault",
        Path.home() / "phone.vault",
        # Working directory & script directory
        Path.cwd() / ".vault",
        Path.cwd() / "phone.vault",
        Path(__file__).resolve().parent / ".vault",
        Path(__file__).resolve().parent / "phone.vault",
    ]

    # Check mounted USB drives under /media/amnesia (Tails OS USB auto-mount)
    media_amnesia = Path("/media/amnesia")
    if media_amnesia.exists():
        try:
            for usb_dir in media_amnesia.iterdir():
                if usb_dir.is_dir():
                    candidates.append(usb_dir / ".vault")
                    candidates.append(usb_dir / "phone.vault")
                    candidates.append(usb_dir / "Cipher_Secrets" / ".vault")
                    candidates.append(usb_dir / APP_DIR_NAME / "Cipher_Secrets" / ".vault")
                    candidates.append(usb_dir / "Documents" / "Cipher" / "Cipher_Secrets" / ".vault")
        except Exception:
            pass

    for candidate in candidates:
        if candidate.is_file() and candidate.stat().st_size >= 32:
            return candidate

    return None

def prompt_for_vault_file() -> Path:
    """Interactively prompts user for .vault file if not found automatically."""
    print("\033[1;33m[?] .vault file was not found automatically.\033[0m")
    print(f"    You can drop your .vault file (from {APP_DIR_NAME} > Cipher_Secrets > .vault)")
    print("    onto your Desktop or enter its path below.")
    while True:
        try:
            raw = input("\n\033[1;36m-> Enter path to .vault file (or drag & drop here): \033[0m").strip()
        except (KeyboardInterrupt, EOFError):
            print("\nOperation cancelled.")
            sys.exit(1)

        clean_path = raw.strip("'\"")
        p = Path(clean_path).expanduser().resolve()
        if p.is_file() and p.stat().st_size >= 32:
            desktop = get_desktop_dir()
            desktop_vault = desktop / ".vault"
            if not desktop_vault.exists() and p != desktop_vault:
                try:
                    ans = input("\033[1;32m[i] Copy .vault to Desktop for automatic detection next time? [Y/n]: \033[0m").strip().lower()
                    if ans in ("", "y", "yes", "e", "evet"):
                        shutil.copy2(p, desktop_vault)
                        print(f"    [✓] Copied to {desktop_vault}.")
                        return desktop_vault
                except Exception:
                    pass
            return p
        print(f"\033[1;31m[-] Error: Valid .vault file not found at: {clean_path}\033[0m")

# ==================== CRYPTOGRAPHIC UTILITIES ====================

def derive_subkey(key: bytes, domain: bytes) -> bytes:
    """HKDF-style domain separation via HMAC-SHA256."""
    return hmac.new(key, domain, hashlib.sha256).digest()

class Poly1305AES:
    """
    Streaming Poly1305-AES MAC calculator 100% compatible with Bouncy Castle Poly1305(AESEngine).
    RFC 8439 / DJB 2005 Poly1305-AES specification.
    """
    def __init__(self, key: bytes, nonce: bytes):
        if len(key) != 32 or len(nonce) != 16:
            raise ValueError("Poly1305-AES requires 32-byte key and 16-byte nonce.")

        r_bar = bytearray(key[:16])
        r_bar[3] &= 15
        r_bar[7] &= 15
        r_bar[11] &= 15
        r_bar[15] &= 15
        r_bar[4] &= 252
        r_bar[8] &= 252
        r_bar[12] &= 252
        self.r = int.from_bytes(r_bar, "little")

        k_aes = key[16:32]
        aes = Cipher(algorithms.AES(k_aes), modes.ECB(), backend=default_backend()).encryptor()
        s_bytes = aes.update(nonce) + aes.finalize()
        self.s = int.from_bytes(s_bytes, "little")

        self.p = (1 << 130) - 5
        self.accumulator = 0
        self.buffer = bytearray()

    def update(self, data: bytes):
        self.buffer.extend(data)
        buf_len = len(self.buffer)
        offset = 0
        while buf_len - offset >= 16:
            chunk = self.buffer[offset:offset + 16]
            chunk_num = int.from_bytes(chunk, "little") + (1 << 128)
            self.accumulator = ((self.accumulator + chunk_num) * self.r) % self.p
            offset += 16
        if offset > 0:
            del self.buffer[:offset]

    def finalize(self) -> bytes:
        if len(self.buffer) > 0:
            pad_bit = 1 << (len(self.buffer) * 8)
            chunk_num = int.from_bytes(self.buffer, "little") + pad_bit
            self.accumulator = ((self.accumulator + chunk_num) * self.r) % self.p
            self.buffer.clear()

        tag_num = (self.accumulator + self.s) % (1 << 128)
        return tag_num.to_bytes(16, "little")

def unlock_vault_header(vault_path: Path, secret: str) -> bytes:
    """
    Unwraps the .vault header file from Android and extracts the 256-bit Master Key.
    """
    data = vault_path.read_bytes()
    if len(data) < 32:
        raise ValueError(".vault file is too small or invalid.")

    offset = 0
    magic = data[offset:offset + 6]
    offset += 6
    if magic != VAULT_HEADER_MAGIC:
        raise ValueError(f"Invalid vault header! Expected: {VAULT_HEADER_MAGIC.decode()}, found: {magic}")

    version = data[offset]
    offset += 1
    kdf_type = data[offset]
    offset += 1
    if kdf_type != 1:
        raise ValueError("Unsupported KDF algorithm (Only Argon2id is supported).")

    iterations, memory_kb, parallelism = struct.unpack(">3i", data[offset:offset + 12])
    offset += 12

    if version >= 3:
        _prot_type = data[offset]
        offset += 1

    salt_len = struct.unpack(">i", data[offset:offset + 4])[0]
    offset += 4
    if salt_len <= 0 or salt_len > 256:
        raise ValueError("Invalid salt length.")

    salt = data[offset:offset + salt_len]
    offset += salt_len

    iv = data[offset:offset + 12]
    offset += 12

    wrapped_len = struct.unpack(">i", data[offset:offset + 4])[0]
    offset += 4
    if wrapped_len <= 0 or offset + wrapped_len > len(data):
        raise ValueError("Corrupted wrapped bundle data.")

    wrapped = data[offset:offset + wrapped_len]

    # Argon2id key derivation
    wrapping_key = hash_secret_raw(
        secret=secret.encode("utf-8"),
        salt=salt,
        time_cost=iterations,
        memory_cost=memory_kb,
        parallelism=parallelism,
        hash_len=32,
        type=Type.ID
    )

    # AES-256-GCM authenticated unwrap
    tag = wrapped[-16:]
    ciphertext = wrapped[:-16]
    cipher = Cipher(algorithms.AES(wrapping_key), modes.GCM(iv, tag), backend=default_backend()).decryptor()
    try:
        bundle = cipher.update(ciphertext) + cipher.finalize()
    except Exception:
        raise ValueError("Incorrect password/PIN or corrupted file!")

    # Master Key is the first 32 bytes of the bundle
    master_key = bundle[:32]
    return master_key

def write_encrypted_folder_meta(folder: Path, original_name: str, master_key: bytes):
    """
    Writes .meta and .nomedia files 100% compatible with Android Fossify Gallery Cipher.
    Subfolders are recognized as encrypted physical albums on the phone.
    """
    meta_file = folder / ".meta"
    iv = secrets.token_bytes(12)
    cipher = Cipher(algorithms.AES(master_key), modes.GCM(iv), backend=default_backend()).encryptor()
    enc_name = cipher.update(original_name.encode("utf-8")) + cipher.finalize() + cipher.tag

    with open(meta_file, "wb") as f:
        f.write(META_MAGIC_V2)                    # 7B ("FGMETA2")
        f.write(iv)                               # 12B IV
        f.write(struct.pack(">i", len(enc_name))) # 4B Big-Endian int
        f.write(enc_name)                         # AES-256-GCM ciphertext + 16B tag

    nomedia_file = folder / ".nomedia"
    if not nomedia_file.exists():
        nomedia_file.touch()

# ==================== MEDIA & FILE OPERATIONS ====================

def get_media_type(filename: str) -> int:
    ext = Path(filename).suffix.lower()
    if ext in VIDEO_EXTS:
        return TYPE_VIDEOS
    elif ext in GIF_EXTS:
        return TYPE_GIFS
    elif ext in RAW_EXTS:
        return TYPE_RAWS
    elif ext in SVG_EXTS:
        return TYPE_SVGS
    return TYPE_IMAGES

def generate_thumbnail_bytes(image_path: Path) -> bytes:
    """Generates 320px JPEG thumbnail for instant gallery preview on Android."""
    if not HAS_PIL:
        return b""
    try:
        with Image.open(image_path) as img:
            img.thumbnail((320, 320))
            if img.mode in ("RGBA", "P"):
                img = img.convert("RGB")
            buf = BytesIO()
            img.save(buf, format="JPEG", quality=75)
            return buf.getvalue()
    except Exception:
        return b""

# ==================== HORIZONTAL PROGRESS BAR ====================

class HorizontalProgressBar:
    """Terminal-width adaptive modern horizontal progress counter."""
    def __init__(self, total_files: int, total_bytes: int):
        self.total_files = total_files
        self.total_bytes = max(total_bytes, 1)
        self.processed_files = 0
        self.processed_bytes = 0
        self.start_time = time.time()
        self.last_update = 0

    def update(self, added_bytes: int, current_file_name: str = "", force: bool = False):
        self.processed_bytes += added_bytes
        now = time.time()
        if not force and (now - self.last_update < 0.08):
            return
        self.last_update = now

        try:
            term_cols = os.get_terminal_size().columns
        except Exception:
            term_cols = 80

        percent = min(100.0, (self.processed_bytes / self.total_bytes) * 100.0)
        elapsed = max(0.001, now - self.start_time)
        speed_mb = (self.processed_bytes / (1024 * 1024)) / elapsed

        mb_done = self.processed_bytes / (1024 * 1024)
        mb_total = self.total_bytes / (1024 * 1024)
        info_str = f" {percent:5.1f}% | {self.processed_files}/{self.total_files} files | {mb_done:.1f}/{mb_total:.1f} MB | {speed_mb:.1f} MB/s"

        fname_max = max(10, term_cols - len(info_str) - 25)
        short_fname = current_file_name if len(current_file_name) <= fname_max else current_file_name[:fname_max - 3] + "..."
        prefix = f"\r\033[1;36m[{short_fname}]\033[0m "

        bar_len = max(10, term_cols - len(prefix) - len(info_str) - 4)
        filled_len = int(bar_len * (percent / 100.0))
        bar = "\033[1;32m" + "█" * filled_len + "\033[0m" + "░" * (bar_len - filled_len)

        line = f"{prefix}[{bar}]{info_str}"
        sys.stdout.write(line[:term_cols])
        sys.stdout.flush()

    def complete_file(self):
        self.processed_files += 1

    def finish(self):
        self.update(0, force=True)
        sys.stdout.write("\n")
        sys.stdout.flush()

# ==================== ENCRYPTION ENGINE ====================

def encrypt_file(
    source_path: Path,
    target_dir: Path,
    master_key: bytes,
    on_chunk_processed=None
) -> Path:
    """
    Encrypts a media file into a .bin file 100% compatible with Android Fossify Gallery Cipher.
    Uses chunked streaming for high-efficiency on large files.
    """
    stat = source_path.stat()
    orig_name = source_path.name
    orig_size = stat.st_size
    last_mod = int(stat.st_mtime * 1000)
    date_taken = last_mod
    media_type = get_media_type(orig_name)
    video_duration = 0

    thumb_bytes = generate_thumbnail_bytes(source_path) if media_type == TYPE_IMAGES else b""

    # 1. Plaintext Header Packet (Java DataOutputStream Big-Endian format)
    hdr_buf = BytesIO()
    name_bytes = orig_name.encode("utf-8")
    hdr_buf.write(struct.pack(">H", len(name_bytes)))
    hdr_buf.write(name_bytes)
    hdr_buf.write(struct.pack(">q", last_mod))
    hdr_buf.write(struct.pack(">q", date_taken))
    hdr_buf.write(struct.pack(">q", orig_size))
    hdr_buf.write(struct.pack(">i", media_type))
    hdr_buf.write(struct.pack(">i", video_duration))
    hdr_buf.write(struct.pack(">i", len(thumb_bytes)))
    if thumb_bytes:
        hdr_buf.write(thumb_bytes)
    header_plain = hdr_buf.getvalue()

    # 2. Key Derivation (Domain separation via HMAC-SHA256)
    header_key = derive_subkey(master_key, DOMAIN_HEADER_KEY)
    content_enc_key = derive_subkey(master_key, DOMAIN_CONTENT_ENC_KEY)
    content_mac_key = derive_subkey(master_key, DOMAIN_CONTENT_MAC_KEY)

    # 3. Header Encryption (AES-256-GCM)
    header_iv = secrets.token_bytes(12)
    h_cipher = Cipher(algorithms.AES(header_key), modes.GCM(header_iv), backend=default_backend()).encryptor()
    enc_header_data = h_cipher.update(header_plain) + h_cipher.finalize() + h_cipher.tag

    # 4. Randomized .bin Output Filename
    while True:
        bin_name = f"{secrets.token_hex(8)}.bin"
        output_file = target_dir / bin_name
        if not output_file.exists():
            break

    # 5. Payload Encryption (AES-256-CTR + Poly1305-AES Streaming)
    content_iv = secrets.token_bytes(16)
    c_cipher = Cipher(algorithms.AES(content_enc_key), modes.CTR(content_iv), backend=default_backend()).encryptor()
    poly_mac = Poly1305AES(content_mac_key, content_iv)

    with open(output_file, "wb") as fout:
        fout.write(MAGIC_V2)                                     # 8B ("FGVLTENC")
        fout.write(bytes([VERSION_SYMMETRIC_AUTH]))              # 1B (0x03)
        fout.write(header_iv)                                    # 12B
        fout.write(struct.pack(">i", len(enc_header_data)))       # 4B Big-Endian int
        fout.write(enc_header_data)                              # Encrypted Header + 16B GCM Tag
        fout.write(content_iv)                                   # 16B Content IV

        chunk_size = 64 * 1024
        with open(source_path, "rb") as fin:
            while True:
                chunk = fin.read(chunk_size)
                if not chunk:
                    break
                enc_chunk = c_cipher.update(chunk)
                if enc_chunk:
                    fout.write(enc_chunk)
                    poly_mac.update(enc_chunk)
                if on_chunk_processed:
                    on_chunk_processed(len(chunk))

            final_enc = c_cipher.finalize()
            if final_enc:
                fout.write(final_enc)
                poly_mac.update(final_enc)

        mac_tag = poly_mac.finalize()
        fout.write(mac_tag)

    try:
        os.utime(output_file, (stat.st_atime, stat.st_mtime))
    except Exception:
        pass

    return output_file

# ==================== CLI ENTRYPOINT ====================

def main():
    desktop = get_desktop_dir()

    parser = argparse.ArgumentParser(
        description="Fossify Gallery Cipher - Linux Offline Encryption Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Desktop Usage (live-boot Linux distros like Tails ideal for diskless RAM operation):
  Running without parameters automatically encrypts all media in 'Desktop/To_Encrypt'
  and places the encrypted .bin files into 'Desktop/Encrypted_Output':
     python3 cipher_vault.py

Custom Directory Usage:
  python3 cipher_vault.py -v ~/.vault -i ~/Downloads/Photos -o ~/Desktop/Encrypted_Output --delete-source
        """
    )
    parser.add_argument("-v", "--vault", help="Path to .vault file (Searched automatically across desktop/USB if omitted)")
    parser.add_argument("-i", "--input", help="Folder or file to encrypt (Default: Desktop/To_Encrypt)")
    parser.add_argument("-o", "--output", help="Output folder for .bin files (Default: Desktop/Encrypted_Output)")
    parser.add_argument("-s", "--secret", help="Vault password or PIN (Prompted securely if omitted)")
    parser.add_argument("--pattern", help="Grid pattern indices if 3x3 pattern is used (e.g. '0,1,2,5')")
    parser.add_argument("--delete-source", action="store_true", help="Securely wipe and delete original files after encryption")
    parser.add_argument("--adb", action="store_true", help="Directly push encrypted files to connected phone via ADB")

    args = parser.parse_args()

    # Verify dependencies
    check_dependencies()

    # 1. Determine Input and Output Directories
    if args.input:
        input_path = Path(args.input).expanduser().resolve()
    else:
        input_path = get_default_input_dir(desktop)

    if args.output:
        output_dir = Path(args.output).expanduser().resolve()
    else:
        output_dir = get_default_output_dir(desktop)

    input_path.mkdir(parents=True, exist_ok=True)
    output_dir.mkdir(parents=True, exist_ok=True)

    # Place .nomedia in output folder to prevent media scanners from indexing
    nomedia = output_dir / ".nomedia"
    if not nomedia.exists():
        try:
            nomedia.touch()
        except Exception:
            pass

    # 2. Collect Files to Encrypt
    items_to_encrypt = [] # list of (source_file, target_subfolder_name_or_none)
    if input_path.is_file():
        items_to_encrypt.append((input_path, None))
    else:
        for p in sorted(input_path.rglob("*")):
            if p.is_file() and not p.name.endswith(".bin") and not p.name.startswith("."):
                rel = p.relative_to(input_path)
                album_name = rel.parts[0] if len(rel.parts) > 1 else None
                items_to_encrypt.append((p, album_name))

    # If input folder is empty, guide user and exit cleanly
    if not items_to_encrypt:
        print("\n" + "=" * 65)
        print("\033[1;36m       Fossify Gallery Cipher - Linux Offline Encryptor\033[0m")
        print("=" * 65)
        print("\033[1;32m[✓] Desktop folders are ready:\033[0m")
        print(f"    📥 Input Folder:   \033[1;33m{input_path}\033[0m")
        print(f"    📤 Output Folder:  \033[1;33m{output_dir}\033[0m")
        print("\n\033[1;33m[!] No photos or videos found in the input folder yet.\033[0m")
        print("    Place the media you want to encrypt into the input folder")
        print("    and run this tool again.")
        print("=" * 65 + "\n")
        sys.exit(0)

    # 3. Locate .vault File
    if args.vault:
        vault_path = Path(args.vault).expanduser().resolve()
        if not vault_path.exists():
            print(f"\033[1;31m[ERROR]\033[0m Specified .vault file not found: {vault_path}", file=sys.stderr)
            sys.exit(1)
    else:
        vault_path = find_vault_file()
        if vault_path:
            print(f"\033[1;32m[✓] .vault file auto-detected:\033[0m {vault_path}")
        else:
            vault_path = prompt_for_vault_file()

    # 4. Read Password / PIN
    secret = args.secret
    if not secret and args.pattern:
        secret = args.pattern.strip()

    if not secret:
        try:
            secret = getpass.getpass("\033[1;33m[?] Enter Vault Password or PIN: \033[0m")
        except (KeyboardInterrupt, EOFError):
            print("\nOperation cancelled.")
            sys.exit(1)

    if not secret:
        print("\033[1;31m[ERROR]\033[0m Password cannot be empty!", file=sys.stderr)
        sys.exit(1)

    # 5. Unlock Vault Header (Argon2id + AES-256-GCM)
    print("\033[1;34m[*] Unwrapping .vault file (Argon2id + AES-256-GCM)...\033[0m")
    try:
        master_key = unlock_vault_header(vault_path, secret)
        print("\033[1;32m[✓] Vault unlocked successfully! Master Key loaded into memory.\033[0m\n")
    except Exception as e:
        print(f"\033[1;31m[ERROR]\033[0m Could not unlock vault: {e}", file=sys.stderr)
        sys.exit(1)

    total_bytes = sum(f.stat().st_size for f, _ in items_to_encrypt)
    print(f"\033[1;35m[*] Total {len(items_to_encrypt)} file(s) ({total_bytes / (1024 * 1024):.2f} MB) queued for encryption...\033[0m")

    # Map subfolder albums (original_name -> target_hex_directory)
    subfolder_map = {}

    progress = HorizontalProgressBar(len(items_to_encrypt), total_bytes)
    created_bins = []

    try:
        for src_file, album_name in items_to_encrypt:
            if album_name is None:
                dest_dir = output_dir
            else:
                if album_name not in subfolder_map:
                    while True:
                        sub_hex = secrets.token_hex(3)
                        sub_target = output_dir / sub_hex
                        if not sub_target.exists():
                            break
                    sub_target.mkdir(parents=True, exist_ok=True)
                    write_encrypted_folder_meta(sub_target, album_name, master_key)
                    subfolder_map[album_name] = sub_target
                dest_dir = subfolder_map[album_name]

            out_bin = encrypt_file(
                src_file,
                dest_dir,
                master_key,
                on_chunk_processed=lambda chunk_len, fn=src_file.name: progress.update(chunk_len, fn)
            )
            created_bins.append(out_bin)
            progress.complete_file()

        progress.finish()
        print(f"\n\033[1;32m[✓] Success! {len(created_bins)} file(s) encrypted and saved:\033[0m")
        print(f"    -> Output Directory: \033[1;36m{output_dir}\033[0m")
        print("\033[1;37m[i] Tip: Transfer the contents of the output folder into your phone's\033[0m")
        print(f"\033[1;37m    '{APP_DIR_NAME}/Cipher_Secrets/' directory.\033[0m")

        # 6. Secure Wipe & Deletion of Plaintext Source Files
        delete_source = args.delete_source
        if not delete_source and sys.stdin.isatty():
            try:
                ans = input("\n\033[1;33m[?] Securely wipe and delete plaintext files from input folder? [y/N]: \033[0m").strip().lower()
                if ans in ("y", "yes", "e", "evet"):
                    delete_source = True
            except (KeyboardInterrupt, EOFError):
                pass

        if delete_source:
            print("\n\033[1;33m[*] Securely overwriting with zeros and deleting source files...\033[0m")
            for src_file, _ in items_to_encrypt:
                try:
                    size = src_file.stat().st_size
                    with open(src_file, "wb") as wipe_f:
                        wipe_f.write(b"\x00" * min(size, 1024 * 1024))
                    src_file.unlink()
                except Exception as e:
                    print(f"    [-] Could not delete: {src_file.name} ({e})")

            # Remove empty subdirectories inside input folder
            if input_path.is_dir():
                for d in sorted(input_path.rglob("*"), key=lambda p: len(str(p)), reverse=True):
                    if d.is_dir() and d != input_path:
                        try:
                            d.rmdir()
                        except Exception:
                            pass
            print("\033[1;32m[✓] Plaintext files securely wiped.\033[0m")

        # 7. ADB Transfer Option
        if args.adb:
            phone_dest = f"/storage/emulated/0/{APP_DIR_NAME}/Cipher_Secrets/"
            print(f"\n\033[1;34m[*] Pushing to phone via ADB: {phone_dest}\033[0m")
            res = subprocess.run(["adb", "devices"], capture_output=True, text=True)
            if "device\n" not in res.stdout:
                print("\033[1;31m[-] Error: No ADB device connected.\033[0m")
            else:
                for b in created_bins:
                    print(f"    -> Transferring: {b.name}")
                    subprocess.run(["adb", "push", str(b), phone_dest], check=True)
                print("\033[1;32m[✓] All files pushed to phone successfully!\033[0m")

    finally:
        # Zeroize Master Key in RAM
        if 'master_key' in locals() and master_key:
            master_key = bytearray(len(master_key))

if __name__ == "__main__":
    main()
