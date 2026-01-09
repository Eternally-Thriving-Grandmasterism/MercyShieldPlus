use ml_kem::fips203::{MlKem768, IndCca2PublicKey, IndCca2SecretKey, IndCca2Ciphertext, IndCca2SharedSecret};
use mldsa::MlDsa65;  // Adjust to exact crate API; pattern: keygen, sign, verify
use rand_core::{OsRng, RngCore};
use zeroize::{Zeroize, Zeroizing};
use aes_gcm::{Aes256Gcm, KeyInit, Nonce};  // For hybrid encryption of larger data
use uniffi::export;

#[derive(ZeroizeOnDrop)]
struct PqKeys {
    kem_sk: IndCca2SecretKey<MlKem768>,
    sig_sk: Vec<u8>,  // ML-DSA secret key bytes
}

#[export]
fn generate_pq_keypair() -> (Vec<u8>, Vec<u8>) {  // Returns (kem_pk, sig_pk) for storage/export
    let (kem_sk, kem_pk) = MlKem768::generate(&mut OsRng);
    let (sig_sk, sig_pk) = MlDsa65::keygen(&mut OsRng);  // Example API
    
    // Persist kem_pk/sig_pk (public only); zeroize sk later
    (kem_pk.to_bytes().to_vec(), sig_pk.to_vec())
}

#[export]
fn pq_sign_attestation(sig_sk_bytes: Vec<u8>, report: Vec<u8>) -> Vec<u8> {  // Sign device integrity JSON
    let sig_sk = /* deserialize safely */;
    let signature = MlDsa65::sign(&sig_sk, &report);
    sig_sk.zeroize();
    signature.to_vec()
}

#[export]
fn pq_verify_attestation(sig_pk_bytes: Vec<u8>, report: Vec<u8>, signature: Vec<u8>) -> bool {
    let sig_pk = /* deserialize */;
    MlDsa65::verify(&sig_pk, &report, &signature)
}

#[export]
fn pq_secure_off_device(report: Vec<u8>, server_kem_pk_bytes: Vec<u8>) -> Vec<u8> {  // Full attestation blob
    let server_pk: IndCca2PublicKey<MlKem768> = /* deserialize server_pk_bytes */;
    
    // Encapsulate: Get shared secret + ciphertext
    let (ciphertext, shared_secret) = MlKem768::encapsulate(&server_pk, &mut OsRng);
    
    // Derive AES-256-GCM key from 32-byte shared secret
    let aes_key = Zeroizing::new(*shared_secret.as_bytes());
    let cipher = Aes256Gcm::new_from_slice(&aes_key).unwrap();
    
    // Nonce (96-bit safe)
    let mut nonce_bytes = [0u8; 12];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = Nonce::from_slice(&nonce_bytes);
    
    // Encrypt report + embed signature (first sign locally if needed)
    let mut encrypted_report = cipher.encrypt(nonce, report.as_ref()).unwrap();
    
    // Blob: ciphertext || nonce || encrypted_report
    let mut blob = ciphertext.to_bytes().to_vec();
    blob.extend_from_slice(&nonce_bytes);
    blob.extend_from_slice(&encrypted_report);
    
    blob
}

// Decaps + decrypt on server side (Rust or other)
