#![no_main]
use libfuzzer_sys::fuzz_target;
use arbitrary::{Arbitrary, Unstructured};
use mercyshieldplus::{pq_verify_data};  // Your lib exports

// Structured input for verify (PK, message, sig — arbitrary bytes)
#[derive(Arbitrary, Debug)]
struct VerifyInput {
    dsa_pk_b64: String,
    message: Vec<u8>,
    signature_b64: String,
}

fuzz_target!(|input: &[u8]| {
    let mut u = Unstructured::new(input);
    if let Ok(verify_input) = VerifyInput::arbitrary(&mut u) {
        // Fuzz verify — panics or logic bugs will crash (detected)
        let _ = pq_verify_data(
            verify_input.dsa_pk_b64,
            verify_input.message,
            verify_input.signature_b64,
        );
    }
});
