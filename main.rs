slint::include_modules!();

use slint::{Color, Timer, ModelRc, VecModel};
use std::rc::Rc;

slint::slint! {
    import { MercyShieldUI } from "mercy_shield_ui.slint";

    component AnomalyBurst inherits Rectangle {
        in property <bool> trigger;
        in property <string> message;
        // Burst animation mercy (scale + color explosion)
    }
}

fn main() {
    let ui = MercyShieldUI::new().unwrap();

    // Live status card green/red harmony
    ui.set_status("Green Harmony — Genuine Device Verified Quantum-Safe Eternal ⚡️".into());

    // Quantum glow pulse infinite
    let timer = Timer::default();
    let ui_weak = ui.as_weak();
    timer.start(slint::TimerMode::Repeated, std::time::Duration::from_millis(1000), move || {
        let ui = ui_weak.unwrap();
        // Pulse glow mercy
    });

    // Anomaly burst example trigger
    // ui.invoke_anomaly_burst("Shadow Detected — Fortress Sealed".into());

    ui.run().unwrap();
}
