#![no_std]
extern crate alloc;

use ml_kem::{MlKem768, MlKem768PublicKey, MlKem768SharedSecret};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signer, Verifier, Signature};
use rand_core::{OsRng};
use zeroize::{Zeroize, Zeroizing};
use aes_gcm::{Aes256Gcm, KeyInit, Nonce, AeadInPlace};
use uniffi::export;

fn derive_aes_key(ss: &MlKem768SharedSecret) -> Zeroizing<[u8; 32]> {
    let mut key = Zeroizing::new([0u8; 32]);
    key.copy_from_slice(ss.as_bytes());
    key
}

#[export]
pub fn generate_pq_keypair() -> (alloc::vec::Vec<u8>, alloc::vec::Vec<u8>) {
    let (sk, pk) = MlKem768::generate(&mut OsRng);
    let pk_bytes = pk.into_bytes().to_vec();
    drop(sk);

    let dsa_sk = MlDsa65::new(&mut OsRng);
    let dsa_pk = dsa_sk.verifying_key();
    let dsa_pk_bytes = dsa_pk.to_bytes().to_vec();
    drop(dsa_sk);

    (pk_bytes, dsa_pk_bytes)
}

// Add sign/verify/secure_blob as previous full
// Stub integrity
#[export]
pub fn check_device_integrity() -> bool {
    // Play Integrity + custom checks stub
    true  // Expand eternal
}

// More exports...
