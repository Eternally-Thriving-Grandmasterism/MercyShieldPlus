// rust/src/integrity.rs — Device Integrity Fortress Eternal (Custom + Play Integrity Token)
use alloc::vec::Vec;
use alloc::string::String;
use uniffi::export;

#[derive(Debug)]
pub enum IntegrityVerdict {
    Genuine,
    Suspicious,
    Compromised,
}

#[derive(Debug)]
pub struct IntegrityReport {
    pub verdict: IntegrityVerdict,
    pub details: Vec<String>,
    pub risk_score: u8,  // 0-100
    pub play_token: String,  // Raw token for server verify
}

/// Evaluate integrity — Kotlin passes evidences + raw Play token
#[export]
pub fn evaluate_integrity(
    suspicious_files: Vec<String>,
    suspicious_props: Vec<String>,
    magisk_indicators: bool,
    play_token: String,  // Raw token string (or "null_token" on fail)
) -> IntegrityReport {
    let mut details = Vec::new();
    let mut score: u8 = 0;

    if !suspicious_files.is_empty() {
        score += 40;
        details.push(format!("Suspicious files: {:?}", suspicious_files));
    }

    if !suspicious_props.is_empty() {
        score += 30;
        details.push(format!("Tamper props: {:?}", suspicious_props));
    }

    if magisk_indicators {
        score += 20;
        details.push("Magisk/Zygisk indicators".to_string());
    }

    // Play token presence (basic client check; server full verify)
    if play_token.is_empty() || play_token == "null_token" {
        score += 35;
        details.push("Play Integrity token unavailable/failed".to_string());
    } else {
        details.push("Play Integrity token acquired — server verify pending".to_string());
        // Client-side basic: no decode needed (server does full)
    }

    score = score.min(100);
    let verdict = if score == 0 {
        IntegrityVerdict::Genuine
    } else if score < 50 {
        IntegrityVerdict::Suspicious
    } else {
        IntegrityVerdict::Compromised
    };

    IntegrityReport {
        verdict,
        details,
        risk_score: score,
        play_token,
    }
}

/// Report to JSON (includes play_token for signing/blob)
#[export]
pub fn report_to_json(report: &IntegrityReport) -> String {
    // Manual JSON mercy (expand with serde later if needed)
    format!(
        r#"{{"verdict":"{:?}","risk_score":{},"details":{},"play_token":"{}"}}"#,
        report.verdict,
        report.risk_score,
        serde_json::to_string(&report.details).unwrap_or("[]".to_string()),
        report.play_token
    )
}
