import CoreLocation
import Foundation

final class SpeedViewModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    enum AccelerationState {
        case ready
        case running
        case complete
    }

    @Published private(set) var speedKmh: Double = 0
    @Published private(set) var maximumSpeedKmh: Double = 0
    @Published private(set) var horizontalAccuracy: CLLocationAccuracy?
    @Published private(set) var accelerationState: AccelerationState = .ready
    @Published private(set) var accelerationSeconds: TimeInterval = 0
    @Published private(set) var permissionDenied = false

    private let locationManager = CLLocationManager()
    private var accelerationStart: Date?
    private var stoppedSince: Date?
    private var displayTimer: Timer?

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.activityType = .automotiveNavigation
        locationManager.distanceFilter = kCLDistanceFilterNone
        locationManager.pausesLocationUpdatesAutomatically = false

        displayTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            self?.updateRunningTimer()
        }

        requestLocationAccess()
    }

    deinit {
        displayTimer?.invalidate()
    }

    func requestLocationAccess() {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            permissionDenied = false
            locationManager.startUpdatingLocation()
        case .denied, .restricted:
            permissionDenied = true
            locationManager.stopUpdatingLocation()
        @unknown default:
            permissionDenied = true
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        requestLocationAccess()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last,
              location.horizontalAccuracy >= 0,
              location.horizontalAccuracy <= 100 else { return }

        let rawSpeed = location.speed >= 0 ? location.speed * 3.6 : 0
        // Suppress the normal 1-2 km/h GPS drift while the vehicle is stationary.
        let displayedSpeed = rawSpeed < 3 ? 0 : rawSpeed
        let now = Date()

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.horizontalAccuracy = location.horizontalAccuracy
            self.speedKmh = displayedSpeed
            self.maximumSpeedKmh = max(self.maximumSpeedKmh, displayedSpeed)
            self.updateAcceleration(using: displayedSpeed, at: now)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        if let locationError = error as? CLError, locationError.code == .denied {
            DispatchQueue.main.async { [weak self] in
                self?.permissionDenied = true
            }
        }
    }

    private func updateAcceleration(using speed: Double, at now: Date) {
        if speed == 0 {
            if stoppedSince == nil {
                stoppedSince = now
            }

            // Two seconds at zero rearms the next 0-100 run and avoids GPS jitter.
            if let stoppedSince, now.timeIntervalSince(stoppedSince) >= 2,
               accelerationState != .ready {
                accelerationState = .ready
                accelerationSeconds = 0
                accelerationStart = nil
            }
            return
        }

        stoppedSince = nil

        if accelerationState == .ready, speed >= 3 {
            accelerationState = .running
            accelerationStart = now
            accelerationSeconds = 0
        }

        if accelerationState == .running,
           let accelerationStart,
           speed >= 100 {
            accelerationSeconds = now.timeIntervalSince(accelerationStart)
            accelerationState = .complete
            self.accelerationStart = nil
        }
    }

    private func updateRunningTimer() {
        guard accelerationState == .running, let accelerationStart else { return }
        accelerationSeconds = Date().timeIntervalSince(accelerationStart)
    }
}
