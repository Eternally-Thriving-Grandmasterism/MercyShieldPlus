// integrity.rs — Device Integrity Fortress Eternal (Custom + Play Integrity Logic)
use alloc::vec::Vec;
use alloc::string::String;
use uniffi::export;

#[derive(Debug)]
pub enum IntegrityVerdict {
    Genuine,      // Hardware-backed mercy
    Suspicious,   // Soft detect
    Compromised,  // Hard root/tampering
}

#[derive(Debug)]
pub struct IntegrityReport {
    pub verdict: IntegrityVerdict,
    pub details: Vec<String>,  // Human-readable mercy messages
    pub risk_score: u8,       // 0-100 (0 = eternal safe)
}

/// Custom root/emulator/tampering checks (pure logic — Kotlin passes evidences)
#[export]
pub fn evaluate_integrity(
    suspicious_files: Vec<String>,     // e.g., ["/system/bin/su", "/magisk"]
    suspicious_props: Vec<String>,     // e.g., ["ro.debuggable=1", "ro.secure=0"]
    magisk_indicators: bool,          // Magisk app/package detect
    play_integrity_verdict: Option<String>,  // "DEVICE_INTEGRITY" / "BASIC_INTEGRITY" etc. from Kotlin
) -> IntegrityReport {
    let mut details = Vec::new();
    let mut score: u8 = 0;

    // File checks
    if !suspicious_files.is_empty() {
        score += 40;
        details.push(format!("Suspicious files detected: {:?}", suspicious_files));
    }

    // Props checks
    if !suspicious_props.is_empty() {
        score += 30;
        details.push(format!("Tamper props: {:?}", suspicious_props));
    }

    if magisk_indicators {
        score += 20;
        details.push("Magisk/Zygisk indicators found".to_string());
    }

    // Play Integrity (Kotlin passes verdict string)
    if let Some(pi) = play_integrity_verdict {
        if pi.contains("MEETS_DEVICE_INTEGRITY") {
            // Genuine mercy
        } else if pi.contains("MEETS_BASIC_INTEGRITY") {
            score += 15;
            details.push("Basic integrity only — possible emulator/soft root".to_string());
        } else {
            score += 35;
            details.push("Play Integrity failed — compromised".to_string());
        }
    } else {
        score += 10;
        details.push("Play Integrity unavailable".to_string());
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
    }
}

// Export report as JSON string for attestation signing
#[export]
pub fn report_to_json(report: IntegrityReport) -> String {
    // Simple serde-like manual (no deps) or add serde feature later
    format!(
        r#"{{"verdict":"{:?}","risk_score":{},"details":{}}}"#,
        report.verdict,
        report.risk_score,
        serde_json::to_string(&report.details).unwrap_or("[]".to_string())
    )
}

// Add more: self-tampering check (signature verify stub)
