#![no_main]
use libfuzzer_sys::fuzz_target;
use arbitrary::{Arbitrary, Unstructured};
use mercyshieldplus::pq_sign_data;

#[derive(Arbitrary, Debug)]
struct SignInput {
    dsa_sk_b64: String,
    message: Vec<u8>,
}

fuzz_target!(|input: &[u8]| {
    let mut u = Unstructured::new(input);
    if let Ok(sign_input) = SignInput::arbitrary(&mut u) {
        let _ = pq_sign_data(sign_input.dsa_sk_b64, sign_input.message);
    }
});
