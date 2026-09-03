import SwiftUI
import UIKit

struct ContentView: View {
    @EnvironmentObject private var speedModel: SpeedViewModel
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color(red: 0.015, green: 0.02, blue: 0.028)
                    .ignoresSafeArea()

                if geometry.size.width > geometry.size.height {
                    landscapeLayout(size: geometry.size)
                } else {
                    portraitLayout(size: geometry.size)
                }

                if speedModel.permissionDenied {
                    permissionOverlay
                }
            }
        }
        .onAppear {
            UIApplication.shared.isIdleTimerDisabled = true
            speedModel.requestLocationAccess()
        }
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                UIApplication.shared.isIdleTimerDisabled = true
                speedModel.requestLocationAccess()
            }
        }
    }

    private func portraitLayout(size: CGSize) -> some View {
        VStack(spacing: 10) {
            header
            Spacer(minLength: 4)
            speedPanel(maximumHeight: size.height * 0.50)
            accelerationPanel
            colorLegend
            footer
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 10)
    }

    private func landscapeLayout(size: CGSize) -> some View {
        VStack(spacing: 4) {
            header
            HStack(spacing: 22) {
                speedPanel(maximumHeight: size.height * 0.67)
                    .frame(maxWidth: .infinity)

                VStack(spacing: 14) {
                    accelerationPanel
                    colorLegend
                    footer
                }
                .frame(width: min(size.width * 0.34, 330))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private var header: some View {
        HStack {
            Label("GPS SPEED", systemImage: "location.fill")
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(.secondary)
            Spacer()
            if let accuracy = speedModel.horizontalAccuracy {
                Text("GPS ±\(Int(accuracy.rounded())) m")
                    .font(.system(size: 13, weight: .medium, design: .monospaced))
                    .foregroundStyle(accuracy <= 20 ? Color.green : Color.yellow)
            } else {
                Text("WAITING FOR GPS")
                    .font(.system(size: 13, weight: .medium, design: .rounded))
                    .foregroundStyle(.yellow)
            }
        }
    }

    private func speedPanel(maximumHeight: CGFloat) -> some View {
        TimelineView(.periodic(from: .now, by: 0.30)) { context in
            let shouldHide = speedModel.speedKmh >= 130 &&
                Int(context.date.timeIntervalSince1970 * 3.33) % 2 == 0

            VStack(spacing: -8) {
                Text(String(Int(speedModel.speedKmh.rounded())))
                    .font(.system(size: maximumHeight * 0.62, weight: .black, design: .rounded))
                    .monospacedDigit()
                    .minimumScaleFactor(0.35)
                    .lineLimit(1)
                    .foregroundStyle(speedColor)
                    .opacity(shouldHide ? 0.12 : 1)
                    .shadow(color: speedColor.opacity(0.42), radius: 18)

                Text("km/h")
                    .font(.system(size: maximumHeight * 0.12, weight: .bold, design: .rounded))
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: maximumHeight)
        }
    }

    private var accelerationPanel: some View {
        VStack(spacing: 5) {
            Text("0–100 km/h")
                .font(.system(size: 17, weight: .bold, design: .rounded))
                .foregroundStyle(.secondary)

            Text(accelerationText)
                .font(.system(size: 42, weight: .black, design: .monospaced))
                .monospacedDigit()
                .foregroundStyle(accelerationColor)

            Text(accelerationStatus)
                .font(.system(size: 13, weight: .bold, design: .rounded))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.white.opacity(0.055))
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(Color.white.opacity(0.10), lineWidth: 1)
                )
        )
    }

    private var colorLegend: some View {
        HStack(spacing: 5) {
            legendSegment("0–80", .white)
            legendSegment("81–99", .green)
            legendSegment("100–109", .yellow)
            legendSegment("110–119", .orange)
            legendSegment("120+", .red)
        }
    }

    private func legendSegment(_ text: String, _ color: Color) -> some View {
        VStack(spacing: 4) {
            Capsule().fill(color).frame(height: 4)
            Text(text)
                .font(.system(size: 9, weight: .bold, design: .rounded))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var footer: some View {
        HStack {
            Text("MAX")
                .foregroundStyle(.secondary)
            Text("\(Int(speedModel.maximumSpeedKmh.rounded())) km/h")
                .monospacedDigit()
                .foregroundStyle(.white)
        }
        .font(.system(size: 15, weight: .bold, design: .rounded))
    }

    private var permissionOverlay: some View {
        VStack(spacing: 12) {
            Image(systemName: "location.slash.fill")
                .font(.system(size: 38))
            Text("Location permission is required")
                .font(.headline)
            Text("Settings → Privacy & Security → Location Services → MotoSpeed")
                .multilineTextAlignment(.center)
                .font(.subheadline)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 22))
        .padding(24)
    }

    private var speedColor: Color {
        switch speedModel.speedKmh {
        case ...80: return .white
        case ..<100: return .green
        case ..<110: return .yellow
        case ..<120: return .orange
        default: return .red
        }
    }

    private var accelerationText: String {
        switch speedModel.accelerationState {
        case .ready:
            return "0.00 s"
        case .running, .complete:
            return String(format: "%.2f s", speedModel.accelerationSeconds)
        }
    }

    private var accelerationStatus: String {
        switch speedModel.accelerationState {
        case .ready: return "READY — START WHEN SAFE"
        case .running: return "MEASURING"
        case .complete: return "RESULT — STOP TO REARM"
        }
    }

    private var accelerationColor: Color {
        switch speedModel.accelerationState {
        case .ready: return .white
        case .running: return .yellow
        case .complete: return .green
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(SpeedViewModel())
}
