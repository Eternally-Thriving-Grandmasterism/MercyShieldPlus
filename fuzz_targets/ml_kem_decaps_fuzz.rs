#![no_main]
use libfuzzer_sys::fuzz_target;
use ml_kem::{MlKem768, MlKem768Ciphertext};

fuzz_target!(|data: &[u8]| {
    if data.len() == MlKem768Ciphertext::BYTE_LEN {
        let ct_bytes: [u8; MlKem768Ciphertext::BYTE_LEN] = data.try_into().unwrap();
        let ct = MlKem768Ciphertext::from_bytes(ct_bytes);
        let (_sk, _pk) = MlKem768::generate(&mut rand::thread_rng());  // Dummy SK
        let _ = MlKem768::decapsulate(&ct, &_sk);  // Fuzz malformed ct
    }
});
