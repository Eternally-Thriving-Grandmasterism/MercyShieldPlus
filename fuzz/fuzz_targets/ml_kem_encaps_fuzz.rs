#![no_main]
use libfuzzer_sys::fuzz_target;
use ml_kem::MlKem768;

fuzz_target!(|server_pk_bytes: &[u8]| {
    if server_pk_bytes.len() == MlKem768PublicKey::BYTE_LEN {
        if let Ok(server_pk) = MlKem768PublicKey::from_bytes(server_pk_bytes.try_into().unwrap()) {
            let _ = MlKem768::encapsulate(&server_pk, &mut rand_core::OsRng);
        }
    }
});
