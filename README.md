Fossify-Gallery-Cipher is a security-hardened personal fork of [Fossify Gallery](https://github.com/FossifyOrg/Gallery), licensed under [GPLv3](LICENSE). See [FORK_CHANGELOG.md](FORK_CHANGELOG.md) for fork changes, and [DOCS.md](DOCS.md) for full architecture and user documentation.

> **Versioning & Upstream Policy:** This fork uses an independent Semantic Versioning scheme (`X.Y.Z-cipher`) originally forked from upstream Fossify Gallery `1.14.1`. Version numbers do not map 1:1 to upstream releases; relevant upstream bug fixes and improvements are selectively backported and documented in [FORK_CHANGELOG.md](FORK_CHANGELOG.md).

# Fossify Gallery Cipher

Unleash memories, not personal data. Fossify Gallery Cipher is a security-hardened fork of the ultimate photo and video app that's as powerful as it is private. No ads, no internet permission, no unnecessary permissions – just a seamless experience tailored for you.

**🖼️ PHOTO EDITING AT YOUR FINGERTIPS:**  
Enhance your photos with our basic yet powerful photo editor. Crop, resize, rotate, flip, draw, and apply stunning filters, all without compromising your privacy. Take control of your memories like never before.

**🌐 PRIVACY FIRST, ALWAYS:**  
Your privacy matters. Ditch the data-hungry giants. Fossify Gallery puts you in control. Strip away EXIF metadata like GPS coordinates and camera details, keeping your memories yours, and yours alone.

**🔒 SUPERIOR SECURITY:**  
Lock down your memories with pin, pattern, or fingerprint protection. Secure specific photos, videos, or the entire app – you decide who gets access. Peace of mind, guaranteed.

**🔐 HARDWARE-HARDENED CRYPTOGRAPHIC VAULT:**  
Go beyond simple folder hiding with an isolated, zero-network cryptographic vault (`Cipher_Secrets`). Media files are individually encrypted on disk using `AES-256-CTR` with `Poly1305-AES` integrity tags, metadata is protected by `AES-256-GCM`, and your Master Encryption Key is derived via `Argon2id` and wrapped by Android Keystore (`StrongBox` / `TEE`). Pre-wipe SHA-256 verification, CSPRNG overwriting, `FLAG_SECURE` screen protection, and automatic RAM zeroization ensure decrypted keys and frames never leak.

**🔗 ZERO-FOOTPRINT VIRTUAL ALBUMS:**  
Organize your media into Virtual Albums without duplicating files or consuming extra storage space. When created from encrypted vault items, they automatically become **Stealth Virtual Albums**—stored with HMAC-obfuscated filenames and `AES-256-GCM` encrypted titles and paths—remaining completely invisible outside the unlocked Secret Vault (accessible via the 3x bottom overscroll gesture).

**🔄 RECOVER WITH EASE:**  
Breathe easy, accidents happen! Fossify Gallery's built-in recycle bin lets you recover deleted photos and videos in seconds. No more lost treasures, just pure relief.

**🎨 YOUR GALLERY, YOUR STYLE:**  
Customize the look, feel, and functionality to match your style. From UI themes to function buttons, Fossify Gallery gives you the creative freedom you crave.

**📷 UNIVERSAL FORMAT FREEDOM:**  
JPEG, JPEG XL, PNG, MP4, MKV, RAW, SVG, GIF, AVIF, videos, and more – we've got your memories covered, in any format you choose. No restrictions, just limitless possibilities.

**✨ MATERIAL DESIGN WITH DYNAMIC THEMES:**  
Experience the beauty of intuitive material design with dynamic themes. Want more? Dive into custom themes and make your gallery truly unique.

---

**Upstream Fossify Project:**  
➡️ Explore more Fossify apps: https://www.fossify.org<br>
➡️ Upstream Open-Source Code: https://www.github.com/FossifyOrg<br>
➡️ Upstream Community on Reddit: https://www.reddit.com/r/Fossify<br>
➡️ Upstream Telegram: https://t.me/Fossify
