use axum::{
    routing::{post},
    Json, Router, extract::State,
};
use ml_kem::{MlKem768, MlKem768Ciphertext, MlKem768SecretKey, MlKem768SharedSecret};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signature, Verifier};
use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use zeroize::Zeroizing;
use reqwest::{Client, header};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::sync::Arc;
use std::net::SocketAddr;

#[derive(Serialize, Deserialize)]
struct AttestationRequest {
    blob: Vec<u8>,  // Raw blob bytes from client (ciphertext || nonce || encrypted_payload)
}

#[derive(Serialize, Deserialize)]
struct TokenVerifyRequest {
    integrity_token: String,
}

#[derive(Serialize, Deserialize)]
struct TokenVerifyResponse {
    token_payload: Value,  // Full decoded payload (JWT body)
}

#[derive(Clone)]
struct AppState {
    server_sk: MlKem768SecretKey,
    http_client: Client,
    play_api_key: String,      // Server-side Play Integrity API key (restricted service account token or OAuth)
    package_name: String,      // Your app package e.g., "com.mercyshieldplus"
}

async fn verify_attestation(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<AttestationRequest>,
) -> Json<String> {
    let blob = payload.blob;

    // Parse blob
    let ct_len = MlKem768Ciphertext::BYTE_LEN;
    if blob.len() < ct_len + 12 {
        return Json("Invalid blob format".to_string());
    }
    let ciphertext_bytes: [u8; MlKem768Ciphertext::BYTE_LEN] = blob[..ct_len].try_into().unwrap();
    let ciphertext = MlKem768Ciphertext::from_bytes(ciphertext_bytes);
    let nonce = Nonce::from_slice(&blob[ct_len..ct_len + 12]);
    let mut encrypted_payload = blob[ct_len + 12..].to_vec();

    // Decapsulate with server SK
    let shared_secret: MlKem768SharedSecret = MlKem768::decapsulate(&ciphertext, &state.server_sk);
    let aes_key = Zeroizing::new(*shared_secret.as_bytes());
    let cipher = Aes256Gcm::new_from_slice(&aes_key).unwrap();

    // Decrypt payload
    if cipher.decrypt_in_place(nonce, b"", &mut encrypted_payload).is_err() {
        return Json("Decryption failed — invalid blob or key".to_string());
    }

    // Parse payload: report JSON + ML-DSA signature (assume sig appended last, fixed size ~3296 for level 65)
    let sig_len = 3296;  // ML-DSA-65 typical sig size — adjust or parse dynamically
    if encrypted_payload.len() < sig_len {
        return Json("Payload too short for signature".to_string());
    }
    let report_bytes = &encrypted_payload[..encrypted_payload.len() - sig_len];
    let signature_bytes = &encrypted_payload[encrypted_payload.len() - sig_len..];

    let report_str = String::from_utf8_lossy(report_bytes);
    let report_json: Value = match serde_json::from_str(&report_str) {
        Ok(json) => json,
        Err(_) => return Json("Invalid report JSON".to_string()),
    };

    // Extract play_token
    let play_token = match report_json["play_token"].as_str() {
        Some(t) if !t.is_empty() && t != "null_token" => t,
        _ => return Json("Missing or invalid Play Integrity token".to_string()),
    };

    // TODO: Verify ML-DSA signature (need client verifying key — stored or sent)
    // let client_pk = MlDsa65::verifying_key_from_bytes(...);
    // if client_pk.verify(report_bytes, &Signature::from_bytes(signature_bytes).unwrap()).is_err() {
    //     return Json("Signature verification failed");
    // }

    // Server-side Play Integrity token verification
    let verify_url = format!("https://playintegrity.googleapis.com/v1/{package}:decodeIntegrityToken", package = state.package_name);

    let verify_body = TokenVerifyRequest {
        integrity_token: play_token.to_string(),
    };

    let response = state.http_client
        .post(&verify_url)
        .bearer_auth(&state.play_api_key)  // OAuth bearer or service account token
        .json(&verify_body)
        .send()
        .await;

    let verdict = match response {
        Ok(resp) if resp.status().is_success() => {
            let verify_resp: TokenVerifyResponse = resp.json().await.unwrap_or(TokenVerifyResponse { token_payload: Value::Null });
            let payload = verify_resp.token_payload;

            // Extract key verdicts (2026 Standard API)
            let device_verdict = payload["deviceIntegrity"]["deviceRecognitionVerdict"]
                .as_array()
                .and_then(|arr| arr.iter().find(|v| v.as_str() == Some("MEETS_DEVICE_INTEGRITY")))
                .is_some();

            let app_verdict = payload["appIntegrity"]["appRecognitionVerdict"]
                .as_str()
                .unwrap_or("") == "PLAY_RECOGNIZED";

            if device_verdict && app_verdict {
                "Genuine Eternal — Device + App integrity confirmed"
            } else {
                "Anomaly — Integrity verdict failed"
            }
        }
        Ok(_) => "Play API error response",
        Err(e) => return Json(format!("Play API request failed: {}", e)),
    };

    // Full mercy response
    Json(format!(
        "Attestation Verdict: {}\nReport: {}\nPlay Token Verdict: {}",
        "PQ Valid (sig pending)", report_str, verdict
    ))
}

#[tokio::main]
async fn main() {
    // Load server SK (env or file mercy — zeroize)
    let server_sk_bytes = std::env::var("SERVER_PQ_SK").expect("SERVER_PQ_SK env var required").into_bytes();
    let server_sk = MlKem768SecretKey::from_bytes(&server_sk_bytes.try_into().unwrap()).unwrap();

    // Play API auth (OAuth bearer token or service account — refresh as needed)
    let play_api_key = std::env::var("PLAY_INTEGRITY_API_KEY").expect("PLAY API key env required");

    let state = Arc::new(AppState {
        server_sk,
        http_client: Client::new(),
        play_api_key,
        package_name: "com.mercyshieldplus".to_string(),
    });

    let app = Router::new()
        .route("/verify", post(verify_attestation))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], 3000));
    println!("Mercy Verifier listening on {}", addr);
    axum::Server::bind(&addr).serve(app.into_make_service()).await.unwrap();
}
