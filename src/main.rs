use pqcrypto_kyber::kyber768::*;
use pqcrypto_traits::kem::{Ciphertext, PublicKey, SecretKey, SharedSecret};

fn main() {
    // Alice generates keypair
    let (pk, sk) = keypair();
    println!("Public Key: {} bytes", pk.as_bytes().len());  // 1184 bytes
    println!("Secret Key: {} bytes", sk.as_bytes().len());  // 2400 bytes

    // Bob encapsulates shared secret using Alice's PK
    let (shared_secret_bob, ciphertext) = encapsulate(&pk);
    println!("Ciphertext: {} bytes", ciphertext.as_bytes().len());  // 1088 bytes
    println!("Shared Secret (Bob): {:?}...", &shared_secret_bob.as_bytes()[..8]);

    // Alice decapsulates to get same shared secret
    let shared_secret_alice = decapsulate(&sk, &ciphertext);
    println!("Shared Secret (Alice): {:?}...", &shared_secret_alice.as_bytes()[..8]);

    assert_eq!(shared_secret_bob.as_bytes(), shared_secret_alice.as_bytes());
    println!("Shared secrets match — quantum-secure KEM success eternal ⚡️");
}
