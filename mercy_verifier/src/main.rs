use axum::{
    routing::{post},
    Json, Router, extract::State,
};
use ml_kem::{MlKem768, MlKem768Ciphertext, MlKem768SecretKey, MlKem768SharedSecret};
use ml_dsa::MlDsa65;
use ml_dsa::signature::{Signature, Verifier};
use aes_gcm::{AeadInPlace, Aes256Gcm, KeyInit, Nonce};
use zeroize::Zeroizing;
use reqwest::{Client};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::sync::Arc;
use std::net::SocketAddr;
use base64::{Engine as _, engine::general_purpose::STANDARD as BASE64};

#[derive(Serialize, Deserialize)]
struct AttestationRequest {
    blob: Vec<u8>,  // Raw blob: ciphertext || nonce || encrypted_payload (payload = json_report || signature)
}

#[derive(Serialize, Deserialize)]
struct TokenVerifyRequest {
    integrity_token: String,
}

#[derive(Serialize, Deserialize)]
struct TokenVerifyResponse {
    token_payload: Value,
}

#[derive(Clone)]
struct AppState {
    server_sk: MlKem768SecretKey,
    http_client: Client,
    play_api_key: String,
    package_name: String,
}

async fn verify_attestation(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<AttestationRequest>,
) -> Json<String> {
    let blob = payload.blob;

    let ct_len = MlKem768Ciphertext::BYTE_LEN;
    if blob.len() < ct_len + 12 {
        return Json("Invalid blob: too short".to_string());
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

    // Parse payload: signed report JSON bytes + detached signature
    let sig_len = 3296;  // ML-DSA-65 typical sig size — adjust dynamically if needed
    if encrypted_payload.len() < sig_len {
        return Json("Payload too short for signature".to_string());
    }

    let signed_data = &encrypted_payload[..encrypted_payload.len() - sig_len];
    let signature_bytes = &encrypted_payload[encrypted_payload.len() - sig_len..];

    let report_str = String::from_utf8_lossy(signed_data);
    let report_json: Value = match serde_json::from_str(&report_str) {
        Ok(j) => j,
        Err(_) => return Json("Invalid report JSON".to_string()),
    };

    // Extract client DSA public key from signed report (bootstrap trust)
    let dsa_pk_base64 = match report_json["dsa_pk_base64"].as_str() {
        Some(s) => s,
        None => return Json("Missing client DSA public key".to_string()),
    };

    let dsa_pk_bytes = match BASE64.decode(dsa_pk_base64) {
        Ok(b) => b,
        Err(_) => return Json("Invalid base64 DSA public key".to_string()),
    };

    let verifying_key = match ml_dsa::VerifyingKey::from_bytes(&dsa_pk_bytes) {
        Ok(vk) => vk,
        Err(_) => return Json("Invalid DSA public key format".to_string()),
    };

    // Verify detached signature
    let signature = match Signature::from_bytes(signature_bytes) {
        Ok(sig) => sig,
        Err(_) => return Json("Invalid signature format".to_string()),
    };

    if verifying_key.verify(signed_data, &signature).is_err() {
        return Json("ML-DSA signature verification failed — forged report".to_string());
    }

    // Signature valid — trust report
    let play_token = match report_json["play_token"].as_str() {
        Some(t) if !t.is_empty() && t != "null_token" => t,
        _ => return Json("Missing valid Play Integrity token".to_string()),
    };

    // Server-side Play Integrity verification
    let verify_url = format!("https://playintegrity.googleapis.com/v1/{}:decodeIntegrityToken", state.package_name);

    let verify_body = TokenVerifyRequest {
        integrity_token: play_token.to_string(),
    };

    let response = state.http_client
        .post(&verify_url)
        .bearer_auth(&state.play_api_key)
        .json(&verify_body)
        .send()
        .await;

    let play_verdict = match response {
        Ok(resp) if resp.status().is_success() => {
            let verify_resp: TokenVerifyResponse = match resp.json().await {
                Ok(v) => v,
                Err(_) => return Json("Failed to parse Play response".to_string()),
            };

            let payload = verify_resp.token_payload;

            let device_ok = payload["deviceIntegrity"]["deviceRecognitionVerdict"]
                .as_array()
                .map(|arr| arr.iter().any(|v| v.as_str() == Some("MEETS_DEVICE_INTEGRITY")))
                .unwrap_or(false);

            let app_ok = payload["appIntegrity"]["appRecognitionVerdict"]
                .as_str()
                .map(|s| s == "PLAY_RECOGNIZED")
                .unwrap_or(false);

            if device_ok && app_ok {
                "Genuine Eternal — Device + App integrity confirmed"
            } else {
                "Anomaly — Play Integrity verdict failed"
            }
        }
        _ => "Play API verification failed",
    };

    // Final mercy response
    Json(format!(
        "PQ + Signature Valid ✓\nPlay Verdict: {}\nReport: {}",
        play_verdict, report_str
    ))
}

#[tokio::main]
async fn main() {
    // Load server ML-KEM secret key (env var hex/base64 mercy)
    let server_sk_hex = std::env::var("SERVER_PQ_SK_HEX").expect("SERVER_PQ_SK_HEX env required");
    let server_sk_bytes = hex::decode(server_sk_hex).expect("Invalid hex SK");
    let server_sk = MlKem768SecretKey::from_bytes(&server_sk_bytes.try_into().unwrap()).unwrap();

    // Play Integrity API key (OAuth bearer)
    let play_api_key = std::env::var("PLAY_INTEGRITY_API_KEY").expect("PLAY API key required");

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
    println!("Mercy Verifier eternal listening on {}", addr);
    axum::Server::bind(&addr).serve(app.into_make_service()).await.unwrap();
}        None => return Json("Missing client DSA public key in report".to_string()),
    };

    let dsa_pk_bytes = match BASE64.decode(dsa_pk_base64) {
        Ok(b) => b,
        Err(_) => return Json("Invalid base64 DSA public key".to_string()),
    };

    let verifying_key = match ml_dsa::VerifyingKey::from_bytes(&dsa_pk_bytes) {
        Ok(vk) => vk,
        Err(_) => return Json("Invalid DSA public key format".to_string()),
    };

    // Verify signature on signed_data (report JSON bytes)
    let signature = match Signature::from_bytes(signature_bytes) {
        Ok(sig) => sig,
        Err(_) => return Json("Invalid signature format".to_string()),
    };

    if verifying_key.verify(signed_data, &signature).is_err() {
        return Json("ML-DSA signature verification failed — forged report".to_string());
    }

    // Signature valid → trust report contents (including PK bootstrap)
    let play_token = match report_json["play_token"].as_str() {
        Some(t) if !t.is_empty() && t != "null_token" => t,
        _ => return Json("Missing valid Play Integrity token after sig verify".to_string()),
    };

    // Server-side Play token verification
    let verify_url = format!("https://playintegrity.googleapis.com/v1/{}:decodeIntegrityToken", state.package_name);

    let verify_body = TokenVerifyRequest {
        integrity_token: play_token.to_string(),
    };

    let response = state.http_client
        .post(&verify_url)
        .bearer_auth(&state.play_api_key)
        .json(&verify_body)
        .send()
        .await;

    let play_verdict = match response {
        Ok(resp) if resp.status().is_success() => {
            let verify_resp: TokenVerifyResponse = match resp.json().await {
                Ok(v) => v,
                Err(_) => return Json("Failed to parse Play response".to_string()),
            };

            let payload = verify_resp.token_payload;

            let device_ok = payload["deviceIntegrity"]["deviceRecognitionVerdict"]
                .as_array()
                .map(|arr| arr.iter().any(|v| v.as_str() == Some("MEETS_DEVICE_INTEGRITY")))
                .unwrap_or(false);

            let app_ok = payload["appIntegrity"]["appRecognitionVerdict"]
                .as_str()
                .map(|s| s == "PLAY_RECOGNIZED")
                .unwrap_or(false);

            if device_ok && app_ok {
                "Genuine Eternal"
            } else {
                "Anomaly Detected"
            }
        }
        _ => "Play API verification failed",
    };

    Json(format!(
        "Signature Valid ✓\nPlay Verdict: {}\nReport: {}",
        play_verdict, report_str
    ))
}

#[tokio::main]
async fn main() {
    let server_sk_bytes = std::env::var("SERVER_PQ_SK").expect("SERVER_PQ_SK required").into_bytes();
    let server_sk = MlKem768SecretKey::from_bytes(&server_sk_bytes.try_into().unwrap()).unwrap();

    let play_api_key = std::env::var("PLAY_INTEGRITY_API_KEY").expect("PLAY API key required");

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
    println!("Mercy Verifier eternal on {}", addr);
    axum::Server::bind(&addr).serve(app.into_make_service()).await.unwrap();
}
