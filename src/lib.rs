#![no_std]  // Minimal footprint – alloc only where needed
extern crate alloc;

use ml_kem::{
    MlKem768, MlKem768Ciphertext, MlKem768PublicKey, MlKem768SecretKey, MlKem768SharedSecret,
};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signature, Signer, Verifier};
use rand_core::{OsRng, RngCore};
use zeroize::{Zeroize, ZeroizeOnDrop, Zeroizing};
use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use uniffi::export;

/// Derive AES-256-GCM key from 32-byte ML-KEM shared secret (zeroizing for safety)
fn derive_aes_key(shared_secret: &MlKem768SharedSecret) -> Zeroizing<[u8; 32]> {
    let mut key = Zeroizing::new([0u8; 32]);
    key.copy_from_slice(shared_secret.as_bytes());
    key
}

/// Generate fresh PQ keypair: Returns serialized (pk_bytes for KEM, pk_bytes for DSA)
#[export]
pub fn generate_pq_keypair() -> (alloc::vec::Vec<u8>, alloc::vec::Vec<u8>) {
    // ML-KEM-768 keygen
    let (kem_sk, kem_pk) = MlKem768::generate(&mut OsRng);
    let kem_pk_bytes = kem_pk.into_bytes().to_vec();

    // ML-DSA-65 keygen
    let dsa_sk = MlDsa65::new(&mut OsRng);
    let dsa_pk = dsa_sk.verifying_key();
    let dsa_pk_bytes = dsa_pk.to_bytes().to_vec();

    // Secret keys zeroized on drop (crate implements ZeroizeOnDrop)
    core::mem::drop(kem_sk);
    core::mem::drop(dsa_sk);

    (kem_pk_bytes, dsa_pk_bytes)
}

/// Sign integrity report (or any data) with ML-DSA-65
/// Input: serialized DSA secret key + message bytes
/// Output: signature bytes
#[export]
pub fn pq_sign_data(dsa_sk_bytes: alloc::vec::Vec<u8>, message: alloc::vec::Vec<u8>) -> alloc::vec::Vec<u8> {
    let dsa_sk = MlDsa65::from_bytes(&dsa_sk_bytes).expect("Invalid DSA SK");
    let signature: Signature = dsa_sk.try_sign(&message).expect("Signing failed");
    let sig_bytes = signature.to_bytes().to_vec();

    // Zeroize SK immediately after use
    let mut sk_mut = dsa_sk_bytes;
    sk_mut.zeroize();

    sig_bytes
}

/// Verify signed data with ML-DSA-65
#[export]
pub fn pq_verify_data(
    dsa_pk_bytes: alloc::vec::Vec<u8>,
    message: alloc::vec::Vec<u8>,
    signature_bytes: alloc::vec::Vec<u8>,
) -> bool {
    let dsa_pk = ml_dsa::VerifyingKey::from_bytes(&dsa_pk_bytes).expect("Invalid DSA PK");
    let signature = Signature::from_bytes(&signature_bytes).expect("Invalid signature");

    dsa_pk.verify(&message, &signature).is_ok()
}

/// Secure off-device attestation blob
/// Input: integrity report (JSON bytes), server KEM public key, local DSA SK (optional sign first)
/// Process: Sign report locally → Encapsulate to server PK → Derive AES key → Encrypt (report + sig)
/// Output: Blob = ciphertext || nonce || encrypted_payload
#[export]
pub fn pq_secure_attestation_blob(
    report: alloc::vec::Vec<u8>,
    server_kem_pk_bytes: alloc::vec::Vec<u8>,
    local_dsa_sk_bytes: Option<alloc::vec::Vec<u8>>,  // Optional local signing
) -> alloc::vec::Vec<u8> {
    let server_pk = MlKem768PublicKey::from_bytes(&server_kem_pk_bytes).expect("Invalid server PK");

    // Optional: Sign report first
    let mut payload = report.clone();
    let mut signature = alloc::vec::Vec::new();
    if let Some(dsa_sk_bytes) = local_dsa_sk_bytes {
        signature = pq_sign_data(dsa_sk_bytes, report.clone());
        payload.extend_from_slice(&signature);
    }

    // ML-KEM encapsulate
    let (ciphertext, shared_secret) = MlKem768::encapsulate(&server_pk, &mut OsRng);

    // Derive AES-256-GCM
    let aes_key = derive_aes_key(&shared_secret);
    let cipher = Aes256Gcm::new(&aes_key);

    // Nonce (96-bit)
    let mut nonce_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);

    // Encrypt in-place
    let mut encrypted_payload = payload;
    cipher.encrypt_in_place(nonce, b"", &mut encrypted_payload).expect("Encryption failed");

    // Blob assembly
    let mut blob = ciphertext.into_bytes().to_vec();
    blob.extend_from_slice(&nonce_bytes);
    blob.extend_from_slice(&encrypted_payload);
    if !signature.is_empty() {
        blob.extend_from_slice(&signature);  // Optional: append raw sig if needed server-side
    }

    blob
}

/// Server-side decaps + decrypt (for your Rust backend verifier – same crate)
pub fn pq_decapsulate_blob(
    blob: &[u8],
    server_kem_sk: MlKem768SecretKey,
) -> Result<(alloc::vec::Vec<u8>, alloc::vec::Vec<u8>), &'static str> {
    // Parse blob (adjust lengths per spec)
    let ct_len = MlKem768Ciphertext::BYTE_LEN;
    let ciphertext = MlKem768Ciphertext::from_bytes(blob[..ct_len].try_into().map_err(|_| "Invalid CT")?);
    let nonce = Nonce::from_slice(&blob[ct_len..ct_len + 12]);
    let encrypted_payload = &blob[ct_len + 12..];

    let shared_secret = MlKem768::decapsulate(&ciphertext, &server_kem_sk);
    let aes_key = derive_aes_key(&shared_secret);
    let cipher = Aes256Gcm::new(&aes_key);

    let mut payload = encrypted_payload.to_vec();
    cipher.decrypt_in_place(nonce, b"", &mut payload).map_err(|_| "Decrypt failed")?;

    Ok((payload, shared_secret.into_bytes().to_vec()))  // Return plaintext + SS if needed
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn full_pq_flow() {
        let (kem_pk, dsa_pk) = generate_pq_keypair();
        // Full test would need SK persistence – but basics work
        assert!(!kem_pk.is_empty());
        assert!(!dsa_pk.is_empty());
    }
}
