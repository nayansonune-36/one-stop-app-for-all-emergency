import {
  AlertTriangle,
  Camera,
  Check,
  Flashlight,
  FlashlightOff,
  RefreshCw,
  ShieldCheck,
  Upload,
  X,
} from "lucide-react";
import { ChangeEvent, useCallback, useEffect, useRef, useState } from "react";
import { Language, translate } from "./i18n";
import "./qr-scanner.css";

type PermissionState = "granted" | "denied" | "prompt";

type BarcodeResult = { rawValue?: string };
type BarcodeDetectorLike = {
  detect(source: ImageBitmapSource): Promise<BarcodeResult[]>;
};
type BarcodeDetectorConstructor = new (options?: { formats?: string[] }) => BarcodeDetectorLike;

type QrScannerProps = {
  language: Language;
  onClose: () => void;
  onDecoded: (data: unknown) => void;
  cameraPermission: PermissionState;
  onPermissionChange?: (state: PermissionState) => void;
};

type ScannerStage = "idle" | "loading" | "scanning" | "review" | "success" | "error";

const medicalFields = [
  "name",
  "age",
  "blood",
  "allergies",
  "conditions",
  "medications",
  "contactName",
  "contactPhone",
  "notes",
] as const;

function getBarcodeDetector(): BarcodeDetectorLike | null {
  const Detector = (window as unknown as { BarcodeDetector?: BarcodeDetectorConstructor })
    .BarcodeDetector;
  if (!Detector) return null;
  return new Detector({ formats: ["qr_code"] });
}

function parseMedicalQr(rawValue: string): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(rawValue) as unknown;
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return null;
    const record = parsed as Record<string, unknown>;
    const payload =
      record.medicalId && typeof record.medicalId === "object" && !Array.isArray(record.medicalId)
        ? (record.medicalId as Record<string, unknown>)
        : record;
    return medicalFields.some((field) => typeof payload[field] === "string") ? payload : null;
  } catch {
    return null;
  }
}

function permissionFromError(error: unknown): PermissionState {
  if (error instanceof DOMException && ["NotAllowedError", "SecurityError"].includes(error.name)) {
    return "denied";
  }
  return "prompt";
}

export default function QrScanner({
  language,
  onClose,
  onDecoded,
  cameraPermission,
  onPermissionChange,
}: QrScannerProps) {
  const t = useCallback((text: string) => translate(language, text), [language]);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const trackRef = useRef<MediaStreamTrack | null>(null);
  const scanTimerRef = useRef<number | null>(null);
  const detectingRef = useRef(false);
  const [stage, setStage] = useState<ScannerStage>(
    cameraPermission === "granted" ? "loading" : "idle",
  );
  const [error, setError] = useState("");
  const [decoded, setDecoded] = useState<Record<string, unknown> | null>(null);
  const [torchAvailable, setTorchAvailable] = useState(false);
  const [torchOn, setTorchOn] = useState(false);
  const [uploading, setUploading] = useState(false);

  const stopCamera = useCallback(() => {
    if (scanTimerRef.current) {
      window.clearInterval(scanTimerRef.current);
      scanTimerRef.current = null;
    }
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    trackRef.current = null;
    setTorchOn(false);
  }, []);

  const acceptRawValue = useCallback(
    (rawValue: string) => {
      const medicalId = parseMedicalQr(rawValue);
      if (!medicalId) {
        setError(
          t(
            "This QR code does not contain supported Medical ID information. Check the code and try again.",
          ),
        );
        setStage("error");
        return false;
      }
      stopCamera();
      setDecoded(medicalId);
      setStage("review");
      return true;
    },
    [stopCamera, t],
  );

  const beginDetection = useCallback(
    (detector: BarcodeDetectorLike) => {
      if (scanTimerRef.current) window.clearInterval(scanTimerRef.current);
      scanTimerRef.current = window.setInterval(async () => {
        const video = videoRef.current;
        if (
          !video ||
          video.readyState < HTMLMediaElement.HAVE_ENOUGH_DATA ||
          detectingRef.current
        ) {
          return;
        }
        detectingRef.current = true;
        try {
          const results = await detector.detect(video);
          const rawValue = results[0]?.rawValue;
          if (rawValue) acceptRawValue(rawValue);
        } catch {
          // Individual frames can fail while the camera is settling; keep scanning.
        } finally {
          detectingRef.current = false;
        }
      }, 650);
    },
    [acceptRawValue],
  );

  const startCamera = useCallback(async () => {
    setStage("loading");
    setError("");
    stopCamera();
    if (!navigator.mediaDevices?.getUserMedia) {
      setError(
        t("Camera access is unavailable in this browser. You can upload a QR image instead."),
      );
      setStage("error");
      onPermissionChange?.("denied");
      return;
    }
    const detector = getBarcodeDetector();
    if (!detector) {
      setError(
        t(
          "Live QR detection is unavailable in this browser. You can still choose a QR image on a supported browser.",
        ),
      );
      setStage("error");
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: "environment" } },
        audio: false,
      });
      streamRef.current = stream;
      const track = stream.getVideoTracks()[0];
      trackRef.current = track;
      const capabilities = track.getCapabilities() as MediaTrackCapabilities & { torch?: boolean };
      setTorchAvailable(Boolean(capabilities?.torch));
      onPermissionChange?.("granted");
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setStage("scanning");
      beginDetection(detector);
    } catch (cameraError) {
      const nextPermission = permissionFromError(cameraError);
      onPermissionChange?.(nextPermission);
      setError(
        nextPermission === "denied"
          ? t(
              "Camera permission was denied. Allow camera access in your browser or device settings, or upload a QR image.",
            )
          : t("The camera could not be started. Check that another app is not using it."),
      );
      setStage("error");
    }
  }, [beginDetection, onPermissionChange, stopCamera, t]);

  useEffect(() => {
    if (cameraPermission === "granted") startCamera();
    return stopCamera;
  }, []);

  const toggleTorch = async () => {
    const track = trackRef.current;
    if (!track || !torchAvailable) return;
    const next = !torchOn;
    try {
      await track.applyConstraints({
        advanced: [{ torch: next } as MediaTrackConstraintSet],
      });
      setTorchOn(next);
    } catch {
      setError(t("Flashlight control is not available on this device."));
    }
  };

  const uploadQrImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    setUploading(true);
    setError("");
    try {
      const detector = getBarcodeDetector();
      if (!detector || !("createImageBitmap" in window)) {
        throw new Error("unsupported");
      }
      const image = await createImageBitmap(file);
      try {
        const results = await detector.detect(image);
        const rawValue = results[0]?.rawValue;
        if (!rawValue) {
          setError(t("No QR code was found in that image. Choose a clearer image and try again."));
          setStage("error");
        } else {
          acceptRawValue(rawValue);
        }
      } finally {
        image.close();
      }
    } catch {
      setError(
        t(
          "This browser cannot read QR images. Try camera scanning or use a browser with QR detection support.",
        ),
      );
      setStage("error");
    } finally {
      setUploading(false);
    }
  };

  const confirmImport = () => {
    if (!decoded) return;
    setStage("success");
    window.setTimeout(() => onDecoded(decoded), 650);
  };

  const closeScanner = () => {
    stopCamera();
    onClose();
  };

  const reviewRows = decoded
    ? medicalFields
        .filter((field) => typeof decoded[field] === "string" && decoded[field])
        .map((field) => ({ field, value: String(decoded[field]) }))
    : [];

  const labels: Record<(typeof medicalFields)[number], string> = {
    name: t("Full name"),
    age: t("Age"),
    blood: t("Blood group"),
    allergies: t("Allergies"),
    conditions: t("Medical conditions"),
    medications: t("Medications"),
    contactName: t("Emergency contact"),
    contactPhone: t("Contact phone"),
    notes: t("Additional notes"),
  };

  return (
    <section className="qr-scanner-screen" aria-label={t("Scan Medical ID QR")}>
      <header className="qr-scanner-header">
        <button type="button" onClick={closeScanner} aria-label={t("Close scanner")}>
          <X />
        </button>
        <div>
          <small>{t("Medical ID")}</small>
          <h1>{t("Scan QR code")}</h1>
        </div>
        <span aria-hidden="true" />
      </header>

      {(stage === "loading" || stage === "scanning") && (
        <div className="qr-camera-panel">
          <video ref={videoRef} muted playsInline aria-label={t("Camera preview")} />
          <div className="qr-scan-shade" aria-hidden="true">
            <span className="qr-scan-frame">
              <i />
            </span>
          </div>
          {stage === "loading" && (
            <div className="qr-camera-loading" role="status">
              <span className="spinner" />
              <strong>{t("Starting camera…")}</strong>
            </div>
          )}
          {stage === "scanning" && (
            <>
              <p className="qr-scan-hint">{t("Place a Medical ID QR code inside the frame")}</p>
              <button
                type="button"
                className={`qr-torch ${torchOn ? "active" : ""}`}
                onClick={toggleTorch}
                disabled={!torchAvailable}
                aria-pressed={torchOn}
              >
                {torchOn ? <FlashlightOff /> : <Flashlight />}
                {torchAvailable
                  ? torchOn
                    ? t("Turn flashlight off")
                    : t("Turn flashlight on")
                  : t("Flashlight unavailable")}
              </button>
            </>
          )}
        </div>
      )}

      {(stage === "idle" || stage === "error") && (
        <div className="qr-access-state">
          <span className={stage === "error" ? "error" : ""}>
            {stage === "error" ? <AlertTriangle /> : <Camera />}
          </span>
          <h2>{stage === "error" ? t("Camera unavailable") : t("Camera access needed")}</h2>
          <p>
            {error ||
              t(
                "Camera access is used only while this screen is open to scan Medical ID QR codes.",
              )}
          </p>
          <button type="button" className="button primary" onClick={startCamera}>
            {stage === "error" ? <RefreshCw /> : <Camera />}
            {stage === "error" ? t("Try camera again") : t("Allow camera")}
          </button>
          {cameraPermission === "denied" && (
            <div className="qr-permission-guidance">
              <strong>{t("Permission guidance")}</strong>
              <p>
                {t(
                  "Open your browser or device settings, allow camera access for this site, then return and try again.",
                )}
              </p>
            </div>
          )}
        </div>
      )}

      {stage === "review" && decoded && (
        <div className="qr-review-state">
          <span className="qr-review-icon">
            <ShieldCheck />
          </span>
          <h2>{t("Medical ID found")}</h2>
          <p>{t("Review the decoded information. Import it only with the person’s consent.")}</p>
          <dl>
            {reviewRows.map(({ field, value }) => (
              <div key={field}>
                <dt>{labels[field]}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
          <button type="button" className="button primary" onClick={confirmImport}>
            <Check /> {t("Consent confirmed · Import Medical ID")}
          </button>
          <button
            type="button"
            className="button secondary"
            onClick={() => {
              setDecoded(null);
              startCamera();
            }}
          >
            {t("Scan another code")}
          </button>
        </div>
      )}

      {stage === "success" && (
        <div className="qr-success-state" role="status">
          <span>
            <Check />
          </span>
          <h2>{t("Medical ID ready")}</h2>
          <p>{t("The decoded information has been reviewed and is ready to use.")}</p>
        </div>
      )}

      {stage !== "review" && stage !== "success" && (
        <div className="qr-upload-card">
          <div>
            <Upload />
            <span>
              <strong>{t("Upload QR image")}</strong>
              <small>{t("Choose a clear image from this device")}</small>
            </span>
          </div>
          <label className={uploading ? "disabled" : ""}>
            {uploading ? <span className="spinner" /> : t("Choose image")}
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={uploadQrImage}
              disabled={uploading}
            />
          </label>
        </div>
      )}

      <p className="qr-privacy-note">
        <ShieldCheck /> {t("Camera frames and uploaded images stay on this device.")}
      </p>
    </section>
  );
}
