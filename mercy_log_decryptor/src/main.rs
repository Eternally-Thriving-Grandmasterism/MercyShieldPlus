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
    let args = Args::parse();

    // Validate passphrase hex
    if args.passphrase_hex.len() != 64 {
        eprintln!("Error: Passphrase must be 64 hex chars (32 bytes)");
        std::process::exit(1);
    }
    let passphrase_bytes = hex::decode(&args.passphrase_hex).expect("Invalid hex passphrase");

    // Read encrypted file
    let encrypted_base64 = fs::read_to_string(&args.input)?;
    let encrypted_data = BASE64.decode(encrypted_base64.trim()).expect("Invalid base64 in file");

    if encrypted_data.len() < 12 + 16 {
        eprintln!("Error: Encrypted data too short");
        std::process::exit(1);
    }

    // Split nonce (first 12 bytes)
    let (nonce_bytes, ciphertext_tag) = encrypted_data.split_at(12);
    let nonce = Nonce::from_slice(nonce_bytes);

    // Decrypt
    let key = aes_gcm::Key::<Aes256Gcm>::from_slice(&passphrase_bytes);
    let cipher = Aes256Gcm::new(key);

    let mut plaintext = ciphertext_tag.to_vec();
    cipher.decrypt_in_place(nonce, b"", &mut plaintext).expect("Decryption failed — wrong passphrase or corrupted file");

    // Pretty JSON output mercy
    let pretty_json = serde_json::to_string_pretty(&serde_json::from_slice::<serde_json::Value>(&plaintext).unwrap_or(serde_json::json!({"error": "Invalid JSON"}))).unwrap();

    fs::write(&args.output, pretty_json)?;
    println!("Decrypted logs written to: {:?}", args.output);

    Ok(())
}
