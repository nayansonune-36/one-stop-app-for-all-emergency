export type PermissionKind = "location" | "camera" | "microphone" | "notifications" | "bluetooth";
export type PermissionState = "granted" | "denied" | "prompt";
export type PermissionStatuses = Record<PermissionKind, PermissionState>;

export type PermissionRequestResult = {
  state: PermissionState;
  error?: string;
};

type PermissionDescriptorName =
  | "geolocation"
  | "camera"
  | "microphone"
  | "notifications"
  | "bluetooth";

const permissionNames: Record<PermissionKind, PermissionDescriptorName> = {
  location: "geolocation",
  camera: "camera",
  microphone: "microphone",
  notifications: "notifications",
  bluetooth: "bluetooth",
};

function browserAvailable() {
  return typeof window !== "undefined" && typeof navigator !== "undefined";
}

function mapBrowserState(state: string): PermissionState {
  if (state === "granted") return "granted";
  if (state === "denied") return "denied";
  return "prompt";
}

async function queryPermission(kind: PermissionKind): Promise<PermissionState> {
  if (!browserAvailable()) return "prompt";

  if (kind === "notifications" && "Notification" in window) {
    const notificationState = Notification.permission;
    if (notificationState === "granted") return "granted";
    if (notificationState === "denied") return "denied";
  }

  if (!navigator.permissions?.query) return "prompt";

  try {
    const descriptor = { name: permissionNames[kind] } as PermissionDescriptor;
    const status = await navigator.permissions.query(descriptor);
    return mapBrowserState(status.state);
  } catch {
    return "prompt";
  }
}

export async function refreshPermissions(): Promise<PermissionStatuses> {
  const kinds: PermissionKind[] = [
    "location",
    "camera",
    "microphone",
    "notifications",
    "bluetooth",
  ];
  const states = await Promise.all(kinds.map((kind) => queryPermission(kind)));
  return {
    location: states[0],
    camera: states[1],
    microphone: states[2],
    notifications: states[3],
    bluetooth: states[4],
  };
}

function unsupported(message: string): PermissionRequestResult {
  return { state: "denied", error: message };
}

function mediaError(error: unknown, fallback: string): PermissionRequestResult {
  const name = error instanceof Error ? error.name : "";
  const message = error instanceof Error ? error.message : fallback;
  return {
    state: name === "NotAllowedError" || name === "SecurityError" ? "denied" : "prompt",
    error: message,
  };
}

export async function requestPermission(kind: PermissionKind): Promise<PermissionRequestResult> {
  if (!browserAvailable()) {
    return unsupported("Permissions are unavailable outside a browser.");
  }

  if (kind === "location") {
    if (!navigator.geolocation) return unsupported("Location is not available in this browser.");
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        () => resolve({ state: "granted" }),
        (error) =>
          resolve({
            state: error.code === error.PERMISSION_DENIED ? "denied" : "prompt",
            error: error.message || "Location access could not be confirmed.",
          }),
        { enableHighAccuracy: false, maximumAge: 0, timeout: 10000 },
      );
    });
  }

  if (kind === "camera") {
    if (!navigator.mediaDevices?.getUserMedia) {
      return unsupported("Camera access is not available in this browser or context.");
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      stream.getTracks().forEach((track) => track.stop());
      return { state: "granted" };
    } catch (error) {
      return mediaError(error, "Camera permission could not be checked.");
    }
  }

  if (kind === "microphone") {
    if (!navigator.mediaDevices?.getUserMedia) {
      return unsupported("Microphone access is not available in this browser or context.");
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: false, audio: true });
      stream.getTracks().forEach((track) => track.stop());
      return { state: "granted" };
    } catch (error) {
      return mediaError(error, "Microphone permission could not be checked.");
    }
  }

  if (kind === "notifications") {
    if (!("Notification" in window)) {
      return unsupported("Notifications are not available in this browser.");
    }
    try {
      const result = await Notification.requestPermission();
      return {
        state: result === "granted" ? "granted" : result === "denied" ? "denied" : "prompt",
        ...(result === "default" ? { error: "Notification permission was not decided." } : {}),
      };
    } catch (error) {
      return {
        state: "prompt",
        error: error instanceof Error ? error.message : "Notification permission request failed.",
      };
    }
  }

  const bluetoothNavigator = navigator as Navigator & {
    bluetooth?: {
      requestDevice: (options: { acceptAllDevices: boolean }) => Promise<unknown>;
    };
  };
  if (!bluetoothNavigator.bluetooth?.requestDevice) {
    return unsupported("Bluetooth access is not available in this browser or context.");
  }
  try {
    await bluetoothNavigator.bluetooth.requestDevice({ acceptAllDevices: true });
    return { state: "granted" };
  } catch (error) {
    const name = error instanceof Error ? error.name : "";
    return {
      state: name === "NotFoundError" || name === "AbortError" ? "prompt" : "denied",
      error: error instanceof Error ? error.message : "Bluetooth access was not approved.",
    };
  }
}
