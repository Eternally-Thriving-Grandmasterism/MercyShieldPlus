use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};
use clap::Parser;
use std::fs;
use std::io::{self, Write};
use std::path::PathBuf;

/// MercyShieldPlus Encrypted Log Decryptor Eternal ⚡️
/// Decrypts .enc.txt exports from the app (AES-256-GCM, device passphrase key)
/// Format: base64(nonce 12 bytes + ciphertext + tag 16 bytes)
/// Passphrase: 32-byte key (input as hex string)
#[derive(Parser, Debug)]
#[command(author, version, about)]
struct Args {
    /// Path to encrypted .enc.txt file
    #[arg(short, long)]
    input: PathBuf,

    /// 32-byte passphrase as hex string (64 chars)
    #[arg(short, long)]
    passphrase_hex: String,

    /// Output decrypted JSON file (default: decrypted_logs.json)
    #[arg(short, long, default_value = "decrypted_logs.json")]
    output: PathBuf,
}

fn main() -> io::Result<()> {
    let
