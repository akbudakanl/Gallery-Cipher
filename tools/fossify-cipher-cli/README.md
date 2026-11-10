# Fossify Gallery Cipher - Linux Offline Encryption CLI Tool

This tool encrypts media files (photos, videos) on Linux into `.bin` format, **100% compatible with the Android Fossify Gallery Cipher** application (using `AES-256-CTR` + `Poly1305-AES` authenticated streaming encryption).

> 💡 **Recommended Environment:** Because live-boot Linux operating systems such as **Tails** do not use local storage disks and run entirely in volatile RAM, they are the most suitable choice for this CLI tool's zero-disk security goal.

By encrypting files on your PC prior to transferring them to mobile, raw plaintext media never touches your mobile device's storage, ensuring that Android's media scanner, thumbnail cache, and filesystem journals never encounter unencrypted files.

---

## 🎯 Desktop (2-Folder) Workflow

The tool automatically manages 2 folders directly on your Desktop (running on a diskless live-boot system like Tails ensures these folders live strictly in RAM):

1. **`To_Encrypt` (Desktop):** Place media downloaded or copied from local/external sources here.
2. **`Encrypted_Output` (Desktop):** Running the tool generates randomized `.bin` files here, encrypted with your vault key.

```
Desktop
├── 📁 To_Encrypt         <-- Drop downloaded plaintext photos/videos here
├── 📁 Encrypted_Output   <-- Encrypted .bin files are generated here
├── 🔑 .vault             <-- Master vault header exported from phone (optional on Desktop)
└── ⚙️ Fossify-Cipher     <-- Double-clickable desktop launcher shortcut
```

---

## 🚀 Linux Quick Setup & Execution

### 1. Install Dependencies
Open a terminal on your Debian-based Linux distribution (e.g., Debian, Ubuntu, or Tails):

```bash
sudo apt update && sudo apt install -y python3-cryptography python3-argon2 python3-pil
```
*(If you use Tails Persistent Storage, you can configure these as Additional Software so they remain installed across reboots).*

### 2. Preparation (One-Time)
Copy the `.vault` file from your phone (`Documents > Cipher > Cipher_Secrets > .vault`) to your Desktop (or Tails Persistent folder).

> ℹ️ **Security Note:** The `.vault` file is encrypted with your vault password/PIN via **Argon2id + AES-256-GCM**. It is mathematically impossible to crack without your password.

### 3. Execution
Navigate to the directory and run:

```bash
chmod +x run.sh cipher_vault.py
./run.sh
```

or directly using Python:
```bash
python3 cipher_vault.py
```

*On first run, the tool automatically creates `To_Encrypt` and `Encrypted_Output` on your Desktop, along with a double-clickable desktop launcher (`Fossify-Cipher.desktop`).*

---

## 💻 Step-by-Step Usage

1. **Place Media:** Drop files or folders into `To_Encrypt` on your Desktop.
2. **Run the Tool:** Double-click the `Fossify-Cipher` desktop launcher or execute `./run.sh`.
3. **Enter Password:** Enter your vault PIN or password when prompted.
4. **Encrypted Output:** The encrypted `.bin` files will be ready in `Encrypted_Output`.
5. **Transfer to Phone:** Move the files from `Encrypted_Output` into your phone's directory:
   ```
   Documents/Cipher/Cipher_Secrets/
   ```
6. **Secure Source Sanitation:** The tool prompts whether to securely overwrite (with zeros) and delete the plaintext source files from the `To_Encrypt` folder (`[y/N]`).

---

## 📂 Subfolder & Album Preservation

If you organize files into subfolders inside `To_Encrypt` (for example: `To_Encrypt/Vacation/photo.jpg` or `To_Encrypt/Documents/scan.png`):

- The tool creates a randomized hex subfolder inside `Encrypted_Output`.
- The original folder name (`Vacation`) is encrypted with **AES-256-GCM** and saved in `.meta`.
- A `.nomedia` file is automatically placed inside.
- When copied into your phone's `Cipher_Secrets/` directory, Fossify Gallery automatically displays it as an encrypted album named **"Vacation"**!

---

## 🛠️ Advanced CLI Options

You can also run the tool with custom flags:

```bash
# Custom input and output directories
python3 cipher_vault.py -v ~/.vault -i ~/Downloads/Photos -o ~/Desktop/Encrypted_Output

# Automatically wipe source files without prompting
python3 cipher_vault.py --delete-source

# Push encrypted files directly to connected phone via ADB
python3 cipher_vault.py --adb

# For vaults locked with a 3x3 pattern (0-8 indices)
python3 cipher_vault.py --pattern "0,1,2,5"
```
