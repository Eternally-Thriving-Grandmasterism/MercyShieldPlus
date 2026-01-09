fn main() {
    uniffi::generate_scaffolding("./src/lib.rs").expect("uniFFI scaffolding generation failed — thunder mercy");
}
