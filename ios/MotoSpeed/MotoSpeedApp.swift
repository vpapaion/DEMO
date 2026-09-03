import SwiftUI
import UIKit

@main
struct MotoSpeedApp: App {
    @StateObject private var speedModel = SpeedViewModel()

    init() {
        UIApplication.shared.isIdleTimerDisabled = true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(speedModel)
                .preferredColorScheme(.dark)
        }
    }
}
