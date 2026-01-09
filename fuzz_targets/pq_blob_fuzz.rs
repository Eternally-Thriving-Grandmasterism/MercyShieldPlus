#![no_main]
use libfuzzer_sys::fuzz_target;
use arbitrary::{Arbitrary, Unstructured};
use mercyshieldplus::{pq_secure_attestation_blob};

#[derive(Arbitrary, Debug)]
struct BlobInput {
    report: Vec<u8>,
    server_kem_pk_b64: String,
    local_dsa_sk_b64: Option<String>,
}

fuzz_target!(|input: &[u8]| {
    let mut u = Unstructured::new(input);
    if let Ok(blob_input) = BlobInput::arbitrary(&mut u) {
        let _ = pq_secure_attestation_blob(
            blob_input.report,
            blob_input.server_kem_pk_b64,
            blob_input.local_dsa_sk_b64,
        );
    }
});
