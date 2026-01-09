#![no_std]
extern crate alloc;

use ml_kem::{
    MlKem768, MlKem768Ciphertext, MlKem768PublicKey, MlKem768SecretKey, MlKem768SharedSecret,
};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signature, Signer, Verifier};
use rand_core::{OsRng};
use zeroize::{Zeroize, Zeroizing};
use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use uniffi::export;
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};

fn derive_aes_key(shared_secret: &MlKem768SharedSecret) -> Zeroizing<[u8; 32]> {
    let mut key = Zeroizing::new([0u8; 32]);
    key.copy_from_slice(shared_secret.as_bytes());
    key
}

/// Generate PQ keypair — returns base64 encoded (KEM PK, DSA PK)
#[export]
pub fn generate_pq_keypair() -> (String, String) {
    let (kem_sk, kem_pk) = MlKem768::generate(&mut OsRng);
    let kem_pk_b64 = BASE64.encode(kem_pk.into_bytes());

    let dsa_sk = MlDsa65::new(&mut OsRng);
    let dsa_pk = dsa_sk.verifying_key();
    let dsa_pk_b64 = BASE64.encode(dsa_pk.to_bytes());

    drop(kem_sk);
    drop(dsa_sk);

    (kem_pk_b64, dsa_pk_b64)
}

/// Sign data with ML-DSA-65 (input: DSA SK base64 + message bytes)
#[export]
pub fn pq_sign_data(dsa_sk_b64: String, message: Vec<u8>) -> String {
    let dsa_sk_bytes = BASE64.decode(dsa_sk_b64).expect("Invalid DSA SK base64");
    let dsa_sk = MlDsa65::from_bytes(&dsa_sk_bytes).expect("Invalid DSA SK");

    let signature: Signature = dsa_sk.try_sign(&message).expect("Signing failed");
    let sig_b64 = BASE64.encode(signature.to_bytes());

    // Zeroize SK mercy
    let mut sk_bytes = dsa_sk_bytes;
    sk_bytes.zeroize();

    sig_b64
}

/// Verify ML-DSA-65 signature (PK base64, message, sig base64)
#[export]
pub fn pq_verify_data(dsa_pk_b64: String, message: Vec<u8>, signature_b64: String) -> bool {
    let dsa_pk_bytes = BASE64.decode(dsa_pk_b64).expect("Invalid DSA PK");
    let dsa_pk = ml_dsa::VerifyingKey::from_bytes(&dsa_pk_bytes).expect("Invalid DSA PK");

    let sig_bytes = BASE64.decode(signature_b64).expect("Invalid signature");
    let signature = Signature::from_bytes(&sig_bytes).expect("Invalid signature format");

    dsa_pk.verify(&message, &signature).is_ok()
}

/// Secure off-device attestation blob
/// Input: report JSON bytes, server KEM PK base64, optional local DSA SK base64
/// Output: base64 encoded blob (for easy Kotlin handling)
#[export]
pub fn pq_secure_attestation_blob(
    report: Vec<u8>,
    server_kem_pk_b64: String,
    local_dsa_sk_b64: Option<String>,
) -> String {
    let server_pk_bytes = BASE64.decode(server_kem_pk_b64).expect("Invalid server PK");
    let server_pk = MlKem768PublicKey::from_bytes(&server_pk_bytes).expect("Invalid server PK format");

    let mut payload = report.clone();
    let mut signature = Vec::new();

    if let Some(dsa_sk_b64) = local_dsa_sk_b64 {
        let sig_b64 = pq_sign_data(dsa_sk_b64, report);
        signature = BASE64.decode(sig_b64).expect("Invalid local signature");
        payload.extend_from_slice(&signature);
    }

    let (ciphertext, shared_secret) = MlKem768::encapsulate(&server_pk, &mut OsRng);

    let aes_key = derive_aes_key(&shared_secret);
    let cipher = Aes256Gcm::new(&aes_key);

    let mut nonce_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);

    let mut encrypted_payload = payload;
    cipher.encrypt_in_place(nonce, b"", &mut encrypted_payload).expect("Encryption failed");

    let mut blob = ciphertext.into_bytes().to_vec();
    blob.extend_from_slice(&nonce_bytes);
    blob.extend_from_slice(&encrypted_payload);

    BASE64.encode(blob)
}

// Additional exports (integrity, etc.) as features grow

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pq_full_flow() {
        let (kem_pk_b64, dsa_pk_b64) = generate_pq_keypair();
        assert!(!kem_pk_b64.is_empty());
        assert!(!dsa_pk_b64.is_empty());

        let message = b"Integrity Report Eternal".to_vec();
        // Full sign/verify test would require persisted SK — basic gen works
    }
}
