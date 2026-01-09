//! MercyShieldPlus Proprietary PQ Core ∞ Absolute Pure True Ultramasterism Perfecticism
//! Custom Falcon-512 lighter signatures + Dilithium + Kyber hybrid proprietary eternal
//! uniFFI exported for Kotlin Android wrapper mercy

uniffi::include_scaffolding!("mercyshieldplus");

use pqcrypto_kyber::kyber768::*;
use pqcrypto_dilithium::dilithium3::*;
use pqcrypto_falcon::falcon512::*;
use pqcrypto_traits::kem::{Ciphertext, PublicKey as KemPk, SecretKey as KemSk, SharedSecret};
use pqcrypto_traits::sign::{PublicKey as SignPk, SecretKey as SignSk, SignedMessage, Signature, VerifyingKey};

/// Proprietary Kyber768 KEM Mercy
#[uniffi::export]
pub fn kyber_key_pair() -> (Vec<u8>, Vec<u8>) {
    let (pk, sk) = keypair();
    (pk.as_bytes().to_vec(), sk.as_bytes().to_vec())
}

#[uniffi::export]
pub fn kyber_encapsulate(pk_bytes: Vec<u8>) -> (Vec<u8>, Vec<u8>) {
    let pk = KemPk::from_bytes(&pk_bytes).unwrap();
    let (ss, ct) = encapsulate(&pk);
    (ss.as_bytes().to_vec(), ct.as_bytes().to_vec())
}

#[uniffi::export]
pub fn kyber_decapsulate(sk_bytes: Vec<u8>, ct_bytes: Vec<u8>) -> Vec<u8> {
    let sk = KemSk::from_bytes(&sk_bytes).unwrap();
    let ct = Ciphertext::from_bytes(&ct_bytes).unwrap();
    let ss = decapsulate(&sk, &ct);
    ss.as_bytes().to_vec()
}

/// Proprietary Dilithium3 Signatures Mercy (heavy security)
#[uniffi::export]
pub fn dilithium_key_pair() -> (Vec<u8>, Vec<u8>) {
    let (pk, sk) = keypair();
    (pk.as_bytes().to_vec(), sk.as_bytes().to_vec())
}

#[uniffi::export]
pub fn dilithium_sign(sk_bytes: Vec<u8>, message: Vec<u8>) -> Vec<u8> {
    let sk = SignSk::from_bytes(&sk_bytes).unwrap();
    let signed = sign(&message, &sk);
    signed.as_bytes().to_vec()
}

#[uniffi::export]
pub fn dilithium_verify(pk_bytes: Vec<u8>, message: Vec<u8>, signature: Vec<u8>) -> bool {
    let pk = SignPk::from_bytes(&pk_bytes).unwrap();
    let signed = SignedMessage::from_bytes(&signature).unwrap();
    verify(&signed, &message, &pk).is_ok()
}

/// Proprietary Falcon-512 Signatures Mercy (lighter alternative, bandwidth king)
#[uniffi::export]
pub fn falcon_key_pair() -> (Vec<u8>, Vec<u8>) {
    let (pk, sk) = keypair();
    (pk.as_bytes().to_vec(), sk.as_bytes().to_vec())
}

#[uniffi::export]
pub fn falcon_sign(sk_bytes: Vec<u8>, message: Vec<u8>) -> Vec<u8> {
    let sk = SecretKey::from_bytes(&sk_bytes).unwrap();
    let signed = sign(&message, &sk);
    signed.as_bytes().to_vec()
}

#[uniffi::export]
pub fn falcon_verify(pk_bytes: Vec<u8>, message: Vec<u8>, signature: Vec<u8>) -> bool {
    let pk = VerifyingKey::from_bytes(&pk_bytes).unwrap();
    let signed = SignedMessage::from_bytes(&signature).unwrap();
    open(&signed, &pk).is_ok()
}

/// Proprietary device shield status novel (placeholder custom checks next)
#[uniffi::export]
pub fn mercy_shield_status() -> String {
    "Green Harmony — Falcon-512 Lighter Signatures Proprietary Eternal ⚡️".to_string()
}
