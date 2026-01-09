#![no_std]
extern crate alloc;

use ml_kem::{
    MlKem768, MlKem768Ciphertext, MlKem768PublicKey, MlKem768SecretKey, MlKem768SharedSecret,
};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signature, Signer, Verifier};
use rand_core::{OsRng, RngCore};
use zeroize::{Zeroize, ZeroizeOnDrop, Zeroizing};
use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use uniffi::{export, Object};

/// Derive AES-256-GCM key from ML-KEM shared secret (zeroizing mercy)
fn derive_aes_key(shared_secret: &MlKem768SharedSecret) -> Zeroizing<[u8; 32]> {
    let mut key = Zeroizing::new([0u8; 32]);
    key.copy_from_slice(shared_secret.as_bytes());
    key
}

/// Generate PQ keypair — returns serialized (KEM PK, DSA PK) bytes
#[export]
pub fn generate_pq_keypair() -> (alloc::vec::Vec<u8>, alloc::vec::Vec<u8>) {
    let (kem_sk, kem_pk) = MlKem768::generate(&mut OsRng);
    let kem_pk_bytes = kem_pk.into_bytes().to_vec();

    let dsa_sk = MlDsa65::new(&mut OsRng);
    let dsa_pk = dsa_sk.verifying_key();
    let dsa_pk_bytes = dsa_pk.to_bytes().to_vec();

    core::mem::drop(kem_sk);  // ZeroizeOnDrop mercy
    core::mem::drop(dsa_sk);

    (kem_pk_bytes, dsa_pk_bytes)
}

/// Sign data with ML-DSA-65 (input: DSA SK bytes + message)
#[export]
pub fn pq_sign_data(dsa_sk_bytes: alloc::vec::Vec<u8>, message: alloc::vec::Vec<u8>) -> alloc::vec::Vec<u8> {
    let dsa_sk = MlDsa65::from_bytes(&dsa_sk_bytes).expect("Invalid DSA SK — fortress sealed");
    let signature: Signature = dsa_sk.try_sign(&message).expect("Signing failed — anomaly");
    let sig_bytes = signature.to_bytes().to_vec();

    let mut sk_mut = dsa_sk_bytes;
    sk_mut.zeroize();  // Immediate zero mercy

    sig_bytes
}

/// Verify ML-DSA-65 signature
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

/// Secure off-device attestation blob (ML-KEM encaps + optional local sign + AES encrypt)
#[export]
pub fn pq_secure_attestation_blob(
    report: alloc::vec::Vec<u8>,
    server_kem_pk_bytes: alloc::vec::Vec<u8>,
    local_dsa_sk_bytes: Option<alloc::vec::Vec<u8>>,
) -> alloc::vec::Vec<u8> {
    let server_pk = MlKem768PublicKey::from_bytes(&server_kem_pk_bytes).expect("Invalid server PK");

    let mut payload = report.clone();
    let mut signature = alloc::vec::Vec::new();
    if let Some(dsa_sk_bytes) = local_dsa_sk_bytes {
        signature = pq_sign_data(dsa_sk_bytes, report);
        payload.extend_from_slice(&signature);
    }

    let (ciphertext, shared_secret) = MlKem768::encapsulate(&server_pk, &mut OsRng);

    let aes_key = derive_aes_key(&shared_secret);
    let cipher = Aes256Gcm::new(&aes_key);

    let mut nonce_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);

    let mut encrypted_payload = payload;
    cipher.encrypt_in_place(nonce, b"", &mut encrypted_payload).expect("Encryption failed — fortress");

    let mut blob = ciphertext.into_bytes().to_vec();
    blob.extend_from_slice(&nonce_bytes);
    blob.extend_from_slice(&encrypted_payload);

    blob
}

// Add more exports: integrity checks chain, anomaly trigger, etc. — thunder next

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pq_flow_eternal() {
        let (kem_pk, dsa_pk) = generate_pq_keypair();
        assert!(!kem_pk.is_empty());
        assert!(!dsa_pk.is_empty());
    }
}    if let Some(dsa_sk_bytes) = local_dsa_sk_bytes {
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
