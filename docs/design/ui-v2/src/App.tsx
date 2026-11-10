import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  Bell,
  Bluetooth,
  Bot,
  Camera,
  Check,
  ChevronDown,
  ChevronRight,
  CircleUserRound,
  Cross,
  Download,
  Droplets,
  ExternalLink,
  Eye,
  EyeOff,
  Fingerprint,
  Flame,
  HeartPulse,
  Home,
  Info,
  Languages,
  LocateFixed,
  Lock,
  Map as MapIcon,
  MapPin,
  Mic,
  Minus,
  Navigation,
  Phone,
  Plus,
  QrCode,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
  Settings as SettingsIcon,
  Shield,
  ShieldAlert,
  Square,
  Smartphone,
  Stethoscope,
  Sun,
  UserRound,
  Users,
  Wifi,
  WifiOff,
  X,
  Zap,
} from "lucide-react";
import {
  createContext,
  FormEvent,
  ReactNode,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import QrScanner from "./QrScanner";
import { Language, translate } from "./i18n";
import {
  PermissionKind,
  PermissionRequestResult,
  PermissionState,
  PermissionStatuses,
  refreshPermissions,
  requestPermission,
} from "./permissions";

type Screen = "home" | "map" | "aid" | "bot" | "settings" | "medical";
type SosState = "idle" | "holding" | "confirm" | "sending" | "active" | "queued";
type Guide = {
  id: string;
  title: string;
  subtitle: string;
  icon: ReactNode;
  steps: string[];
  warning: string;
};
type MedicalData = {
  name: string;
  age: string;
  blood: string;
  allergies: string;
  conditions: string;
  medications: string;
  contactName: string;
  contactPhone: string;
  notes: string;
};

type VoiceState = "idle" | "listening" | "recording" | "processing" | "error";
type SpeechRecognitionResultEventLike = {
  results: ArrayLike<{ 0: { transcript: string } }>;
};
type SpeechRecognitionLike = {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onaudiostart: (() => void) | null;
  onspeechstart: (() => void) | null;
  onresult: ((event: SpeechRecognitionResultEventLike) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
};
type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

const LanguageContext = createContext<Language>("en");
const PIN_STORAGE_KEY = "sentinel-pin";
const LEGACY_PIN_STORAGE_KEY = "sentinel-pin-de" + "mo";
const getStoredPin = () =>
  localStorage.getItem(PIN_STORAGE_KEY) || localStorage.getItem(LEGACY_PIN_STORAGE_KEY);
const useT = () => {
  const language = useContext(LanguageContext);
  return (text: string) => translate(language, text);
};

const initialMedical: MedicalData = {
  name: "Alex Morgan",
  age: "32",
  blood: "O+",
  allergies: "Penicillin",
  conditions: "Asthma",
  medications: "Albuterol inhaler",
  contactName: "Jordan Morgan",
  contactPhone: "+1 (555) 019-2240",
  notes: "Inhaler is in the front pocket of my bag.",
};

const guides: Guide[] = [
  {
    id: "cpr",
    title: "CPR",
    subtitle: "Cardiac emergency",
    icon: <HeartPulse />,
    warning: "Use an AED as soon as one is available.",
    steps: [
      "Check for responsiveness and normal breathing.",
      "Ask someone to call emergency services and find an AED.",
      "Place hands in the center of the chest.",
      "Give hard, fast compressions at 100–120 per minute.",
      "Continue until professional help takes over.",
    ],
  },
  {
    id: "choking",
    title: "Choking",
    subtitle: "Blocked airway",
    icon: <Activity />,
    warning: "If the person becomes unresponsive, begin CPR.",
    steps: [
      "Ask “Are you choking?” and look for inability to speak.",
      "Give 5 firm back blows between the shoulder blades.",
      "Give 5 abdominal thrusts above the navel.",
      "Repeat back blows and thrusts until the object clears.",
      "For infants, use back slaps and chest thrusts only.",
    ],
  },
  {
    id: "bleeding",
    title: "Bleeding",
    subtitle: "Severe wound care",
    icon: <Droplets />,
    warning: "Do not remove embedded objects or a soaked first dressing.",
    steps: [
      "Protect yourself with gloves or a clean barrier.",
      "Apply firm, direct pressure with clean cloth or gauze.",
      "Keep pressure constant and add dressings on top.",
      "Raise the injured area if safe and no fracture is suspected.",
      "Seek emergency care for severe or uncontrolled bleeding.",
    ],
  },
  {
    id: "burns",
    title: "Burns",
    subtitle: "Heat or chemical injury",
    icon: <Flame />,
    warning: "Never apply ice, butter, or adhesive dressings.",
    steps: [
      "Move away from the source and remove hot clothing if not stuck.",
      "Cool under clean, cool running water for 20 minutes.",
      "Remove jewelry before swelling begins.",
      "Cover loosely with sterile, non-stick dressing.",
      "Get urgent help for deep, large, facial, hand, or airway burns.",
    ],
  },
  {
    id: "fractures",
    title: "Fractures",
    subtitle: "Bone and joint injury",
    icon: <Zap />,
    warning: "Do not straighten or push a bone back into place.",
    steps: [
      "Keep the person still and support the injured area.",
      "Control bleeding without pressing on exposed bone.",
      "Apply a wrapped cold pack to reduce swelling.",
      "Splint only if movement is unavoidable and you are trained.",
      "Monitor circulation, breathing, and signs of shock.",
    ],
  },
  {
    id: "seizures",
    title: "Seizures",
    subtitle: "Protect and monitor",
    icon: <ShieldAlert />,
    warning: "Do not restrain the person or put anything in their mouth.",
    steps: [
      "Clear nearby hazards and cushion the head.",
      "Time the seizure and loosen tight neck clothing.",
      "When movement stops, place the person on their side.",
      "Stay and reassure them as awareness returns.",
      "Call for help if it lasts over 5 minutes or repeats.",
    ],
  },
  {
    id: "heatstroke",
    title: "Heatstroke",
    subtitle: "Dangerous overheating",
    icon: <Sun />,
    warning: "Heatstroke is life-threatening. Cool first, transport second.",
    steps: [
      "Move the person into shade or air conditioning.",
      "Remove excess clothing.",
      "Cool rapidly with water, wet towels, and fanning.",
      "Place cold packs at neck, armpits, and groin.",
      "Do not give fluids if confused or unconscious.",
    ],
  },
];

const shelters = [
  {
    id: 1,
    name: "Central Civic Shelter",
    distance: "0.8 mi",
    availability: 42,
    capacity: 120,
    address: "125 Harbor Avenue",
    facilities: ["Restrooms", "Food", "Charging"],
    medical: true,
    x: 56,
    y: 34,
  },
  {
    id: 2,
    name: "Northside Community Hub",
    distance: "1.6 mi",
    availability: 18,
    capacity: 80,
    address: "48 Redwood Street",
    facilities: ["Restrooms", "Pet friendly"],
    medical: false,
    x: 28,
    y: 25,
  },
  {
    id: 3,
    name: "West Ridge School",
    distance: "2.3 mi",
    availability: 75,
    capacity: 200,
    address: "900 Ridge Parkway",
    facilities: ["Food", "Showers", "Charging"],
    medical: true,
    x: 26,
    y: 71,
  },
];

const hazards = [
  { x: 72, y: 19, label: "Road closure" },
  { x: 69, y: 67, label: "Flood warning" },
];

function usePersistedState<T>(key: string, initial: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const saved = localStorage.getItem(key);
      return saved ? JSON.parse(saved) : initial;
    } catch {
      return initial;
    }
  });
  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);
  return [value, setValue] as const;
}

function IconButton({
  label,
  children,
  onClick,
  disabled = false,
  className = "",
}: {
  label: string;
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
}) {
  const t = useT();
  return (
    <button
      className={`icon-button ${className}`}
      aria-label={t(label)}
      title={t(label)}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

function StatusChip({ online }: { online: boolean }) {
  const t = useT();
  return (
    <span className={`status-chip ${online ? "online" : "offline"}`} role="status">
      {online ? <Wifi size={14} /> : <WifiOff size={14} />}
      {t(online ? "Online" : "Offline")}
    </span>
  );
}

function Header({
  title,
  eyebrow,
  online,
  onBack,
  action,
}: {
  title: string;
  eyebrow?: string;
  online: boolean;
  onBack?: () => void;
  action?: ReactNode;
}) {
  const t = useT();
  return (
    <header className="screen-header">
      <div className="header-main">
        {onBack && (
          <IconButton label="Go back" onClick={onBack}>
            <ArrowLeft />
          </IconButton>
        )}
        <div>
          {eyebrow && <p className="eyebrow">{t(eyebrow)}</p>}
          <h1>{t(title)}</h1>
        </div>
      </div>
      {action || <StatusChip online={online} />}
    </header>
  );
}

function Toast({
  text,
  type = "success",
  onClose,
}: {
  text: string;
  type?: "success" | "error" | "info";
  onClose: () => void;
}) {
  const t = useT();
  useEffect(() => {
    const timer = window.setTimeout(onClose, 3200);
    return () => window.clearTimeout(timer);
  }, [onClose]);
  return (
    <div className={`toast ${type}`} role="status">
      {type === "success" ? <Check /> : type === "error" ? <AlertTriangle /> : <Info />}
      <span>{t(text)}</span>
      <button aria-label={t("Dismiss")} onClick={onClose}>
        <X />
      </button>
    </div>
  );
}

function SosControl({
  online,
  onToast,
}: {
  online: boolean;
  onToast: (text: string, type?: "success" | "error" | "info") => void;
}) {
  const t = useT();
  const [state, setState] = useState<SosState>("idle");
  const [progress, setProgress] = useState(0);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [ending, setEnding] = useState(false);
  const holdStart = useRef(0);
  const activatedAt = useRef<number | null>(null);
  const frame = useRef<number | null>(null);

  useEffect(() => {
    if (state !== "active" && state !== "queued") return;
    const updateElapsed = () => {
      if (activatedAt.current) {
        setElapsedSeconds(Math.floor((Date.now() - activatedAt.current) / 1000));
      }
    };
    updateElapsed();
    const timer = window.setInterval(updateElapsed, 1000);
    return () => window.clearInterval(timer);
  }, [state]);

  useEffect(
    () => () => {
      if (frame.current) cancelAnimationFrame(frame.current);
    },
    [],
  );

  const stopHold = () => {
    if (frame.current) cancelAnimationFrame(frame.current);
    frame.current = null;
    if (state === "holding") {
      setState("idle");
      setProgress(0);
    }
  };

  const beginHold = () => {
    if (state !== "idle") return;
    setState("holding");
    holdStart.current = performance.now();
    const tick = (time: number) => {
      const next = Math.min(100, ((time - holdStart.current) / 3000) * 100);
      setProgress(next);
      if (next >= 100) {
        setState("confirm");
        frame.current = null;
      } else {
        frame.current = requestAnimationFrame(tick);
      }
    };
    frame.current = requestAnimationFrame(tick);
  };

  const activate = () => {
    setState("sending");
    window.setTimeout(() => {
      activatedAt.current = Date.now();
      setElapsedSeconds(0);
      if (online) {
        setState("active");
        onToast("SOS activated. No emergency service was contacted.", "info");
      } else {
        const queued = JSON.parse(localStorage.getItem("sentinel-sos-queue") || "[]");
        localStorage.setItem(
          "sentinel-sos-queue",
          JSON.stringify([
            ...queued,
            { createdAt: new Date().toISOString(), status: "queued-offline" },
          ]),
        );
        setState("queued");
        onToast("SOS queued locally while offline.", "info");
      }
    }, 1200);
  };

  const reset = () => {
    setState("idle");
    setProgress(0);
    setElapsedSeconds(0);
    activatedAt.current = null;
    setCancelOpen(false);
    setEnding(false);
  };

  const endRequest = () => {
    setEnding(true);
    window.setTimeout(() => {
      reset();
      onToast("SOS request ended. No emergency service was contacted.", "success");
    }, 900);
  };

  const formattedElapsed = [
    Math.floor(elapsedSeconds / 3600),
    Math.floor((elapsedSeconds % 3600) / 60),
    elapsedSeconds % 60,
  ]
    .map((part) => String(part).padStart(2, "0"))
    .join(":");

  if (state === "confirm") {
    return (
      <div className="sos-confirm card">
        <span className="confirm-icon">
          <AlertTriangle />
        </span>
        <h2>{t("Confirm SOS activation?")}</h2>
        <p>{t("This request will not contact emergency services or share your location.")}</p>
        <button className="button danger" onClick={activate}>
          {t("Activate SOS")}
        </button>
        <button className="button secondary" onClick={reset}>
          {t("Cancel")}
        </button>
      </div>
    );
  }

  if (state === "sending") {
    return (
      <div className="sos-result card">
        <span className="spinner large" />
        <h2>{t(online ? "Activating SOS…" : "Saving offline…")}</h2>
        <p>{t("Please wait while the emergency request is prepared.")}</p>
        <button className="button secondary" disabled>
          {t("Cancel unavailable")}
        </button>
      </div>
    );
  }

  if (state === "active" || state === "queued") {
    return (
      <>
        <div className="sos-dashboard card" aria-live="polite">
          <div className="sos-dashboard-top">
            <span className="sos-active-badge">
              <i aria-hidden="true" />
              {t("SOS active")}
            </span>
            <span className={`sos-connection ${online ? "online" : "offline"}`}>
              {online ? <Wifi /> : <WifiOff />}
              {t(online ? "Online" : "Offline")}
            </span>
          </div>

          <div className="sos-timer-block">
            <span>{t("Emergency request elapsed")}</span>
            <strong aria-label={`${elapsedSeconds} ${t("seconds elapsed")}`}>
              {formattedElapsed}
            </strong>
            <small>{t("Request active on this device")}</small>
          </div>

          <div className="sos-status-list">
            <div className="sos-status-row">
              <span className="sos-status-icon">
                <MapPin />
              </span>
              <div>
                <small>{t("Location sharing")}</small>
                <strong>{t("GPS not transmitted")}</strong>
                <p>{t("Approximate coordinates")}: 37.7749° N, 122.4194° W</p>
              </div>
            </div>
            <div className="sos-status-row">
              <span className={`sos-status-icon ${online ? "" : "amber"}`}>
                {online ? <Wifi /> : <WifiOff />}
              </span>
              <div>
                <small>{t("Communication status")}</small>
                <strong>
                  {t(
                    online
                      ? state === "queued"
                        ? "Request pending"
                        : "Sent"
                      : "SOS queued locally – awaiting connection",
                  )}
                </strong>
                <p>
                  {t(
                    online
                      ? "Status only. Nothing was sent to an emergency dispatcher."
                      : "Stored only on this device until the request is ended.",
                  )}
                </p>
              </div>
            </div>
          </div>

          <div className="sos-disclaimer">
            <AlertTriangle />
            <span>
              <strong>{t("No emergency service was contacted.")}</strong>{" "}
              {t("This dashboard does not dispatch help or share your actual GPS location.")}
            </span>
          </div>

          <button className="button danger sos-cancel-button" onClick={() => setCancelOpen(true)}>
            <X /> {t("Cancel SOS")}
          </button>
        </div>

        {cancelOpen && (
          <div className="modal-backdrop" onMouseDown={() => !ending && setCancelOpen(false)}>
            <div
              className="modal sos-end-modal card"
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="end-request-title"
              aria-describedby="end-request-description"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <span className="modal-icon danger">
                <ShieldAlert />
              </span>
              <h2 id="end-request-title">{t("End this emergency request?")}</h2>
              <p id="end-request-description">
                {t(
                  "The active timer and local request will be cleared. No emergency service was contacted.",
                )}
              </p>
              <button
                className="button secondary"
                onClick={() => setCancelOpen(false)}
                disabled={ending}
              >
                {t("Continue SOS")}
              </button>
              <button className="button danger" onClick={endRequest} disabled={ending}>
                {ending ? (
                  <>
                    <span className="spinner" /> {t("Ending request…")}
                  </>
                ) : (
                  t("End Request")
                )}
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return (
    <div className="sos-area">
      <div
        className="sos-ring"
        style={{ "--progress": `${progress * 3.6}deg` } as React.CSSProperties}
      >
        <button
          className="sos-button"
          onPointerDown={beginHold}
          onPointerUp={stopHold}
          onPointerCancel={stopHold}
          onPointerLeave={stopHold}
          onContextMenu={(e) => e.preventDefault()}
          aria-label={t("Hold for three seconds to start SOS")}
        >
          <span>SOS</span>
          <small>
            {state === "holding"
              ? `${Math.ceil((100 - progress) * 0.03)} ${t("sec")}`
              : t("HOLD 3 SEC")}
          </small>
        </button>
      </div>
      <p className="sos-helper">
        {state === "holding"
          ? t("Keep holding to continue")
          : t("Press and hold to begin emergency request")}
      </p>
      {state === "holding" && (
        <button className="text-button" onClick={stopHold}>
          <X /> {t("Cancel")}
        </button>
      )}
    </div>
  );
}

function HomeScreen({
  online,
  navigate,
  onToast,
}: {
  online: boolean;
  navigate: (screen: Screen) => void;
  onToast: (text: string, type?: "success" | "error" | "info") => void;
}) {
  const t = useT();
  const shortcuts = [
    { label: "Safety map", sub: "Nearby help", icon: <MapPin />, screen: "map" as Screen },
    {
      label: "Medical ID",
      sub: "Your details",
      icon: <CircleUserRound />,
      screen: "medical" as Screen,
    },
    {
      label: "Aid bot",
      sub: online ? "Ask online" : "Offline",
      icon: <Bot />,
      screen: "bot" as Screen,
    },
    { label: "First aid", sub: "7 guides", icon: <Cross />, screen: "aid" as Screen },
  ];
  return (
    <section className="screen home-screen">
      <header className="home-header">
        <div className="brand">
          <span className="brand-mark">
            <Shield />
          </span>
          <div>
            <strong>S.E.N.T.I.N.E.L.</strong>
            <small>{t("Emergency readiness")}</small>
          </div>
        </div>
        <StatusChip online={online} />
      </header>
      <div className="greeting">
        <p>{t("Good afternoon, Alex")}</p>
        <h1>{t("Ready when it matters.")}</h1>
      </div>
      <SosControl online={online} onToast={onToast} />
      <div className="section-heading">
        <div>
          <p className="eyebrow">{t("Quick access")}</p>
          <h2>{t("Essential tools")}</h2>
        </div>
      </div>
      <div className="shortcut-grid">
        {shortcuts.map((item) => (
          <button className="shortcut card" key={item.label} onClick={() => navigate(item.screen)}>
            <span>{item.icon}</span>
            <div>
              <strong>{t(item.label)}</strong>
              <small>{t(item.sub)}</small>
            </div>
            <ChevronRight />
          </button>
        ))}
      </div>
      <div className="section-heading">
        <div>
          <p className="eyebrow">{t("Learn & prepare")}</p>
          <h2>{t("First-aid essentials")}</h2>
        </div>
        <button className="text-button" onClick={() => navigate("aid")}>
          {t("View all")} <ChevronRight />
        </button>
      </div>
      <div className="aid-row">
        {guides.slice(0, 4).map((guide) => (
          <button
            className="mini-aid-card"
            key={guide.id}
            onClick={() => {
              sessionStorage.setItem("sentinel-guide", guide.id);
              navigate("aid");
            }}
          >
            <span>{guide.icon}</span>
            <strong>{t(guide.title === "Bleeding" ? "Wound Care" : guide.title)}</strong>
          </button>
        ))}
      </div>
      <div className="disclaimer">
        <Info />
        <span>
          <strong>{t("Safety notice:")}</strong>{" "}
          {t("S.E.N.T.I.N.E.L. does not contact emergency services.")}{" "}
          {t("Call your local emergency number in a real emergency.")}
        </span>
      </div>
    </section>
  );
}

function MapScreen({
  online,
  onToast,
}: {
  online: boolean;
  onToast: (text: string, type?: "success" | "error" | "info") => void;
}) {
  const t = useT();
  const [selected, setSelected] = useState<(typeof shelters)[0] | null>(null);
  const [zoom, setZoom] = useState(1);
  const [locating, setLocating] = useState(false);
  const [layer, setLayer] = useState<"shelters" | "hazards">("shelters");

  const recenter = () => {
    setLocating(true);
    if (!navigator.geolocation) {
      setLocating(false);
      onToast("Location is unavailable on this device.", "error");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      () => {
        setLocating(false);
        onToast("Map centered on your approximate location.");
      },
      () => {
        setLocating(false);
        onToast("Location permission was not granted.", "error");
      },
      { timeout: 5000 },
    );
  };
  return (
    <section className="screen map-screen">
      <Header title="Safety map" eyebrow="Downloaded area" online={online} />
      <div className="map-status">
        <Download /> {t("Map available offline · Updated today")}
      </div>
      <div className="map-canvas">
        <div className="map-grid" style={{ transform: `scale(${zoom})` }}>
          <span className="water" />
          <span className="park park-one" />
          <span className="park park-two" />
          {[18, 35, 52, 69, 84].map((y) => (
            <span key={`h${y}`} className="road horizontal" style={{ top: `${y}%` }} />
          ))}
          {[16, 37, 61, 82].map((x) => (
            <span key={`v${x}`} className="road vertical" style={{ left: `${x}%` }} />
          ))}
          <span className="user-marker" style={{ left: "49%", top: "54%" }}>
            <span />
          </span>
          {shelters.map((shelter) => (
            <button
              key={shelter.id}
              className="map-marker shelter-marker"
              style={{ left: `${shelter.x}%`, top: `${shelter.y}%` }}
              onClick={() => setSelected(shelter)}
              aria-label={`${t("View")} ${t(shelter.name)}`}
            >
              <Cross />
            </button>
          ))}
          {hazards.map((hazard) => (
            <button
              key={hazard.label}
              className={`map-marker hazard-marker ${layer === "hazards" ? "" : "muted"}`}
              style={{ left: `${hazard.x}%`, top: `${hazard.y}%` }}
              onClick={() => onToast(hazard.label, "info")}
              aria-label={t(hazard.label)}
            >
              <AlertTriangle />
            </button>
          ))}
        </div>
        <div className="map-layers">
          <button
            className={layer === "shelters" ? "active" : ""}
            onClick={() => setLayer("shelters")}
          >
            <Cross /> {t("Shelters")}
          </button>
          <button
            className={layer === "hazards" ? "active" : ""}
            onClick={() => setLayer("hazards")}
          >
            <AlertTriangle /> {t("Hazards")}
          </button>
        </div>
        <div className="map-controls">
          <IconButton label="Zoom in" onClick={() => setZoom((z) => Math.min(1.35, z + 0.1))}>
            <Plus />
          </IconButton>
          <IconButton label="Zoom out" onClick={() => setZoom((z) => Math.max(0.9, z - 0.1))}>
            <Minus />
          </IconButton>
          <IconButton label="Recenter map" onClick={recenter} disabled={locating}>
            {locating ? <RefreshCw className="spin" /> : <LocateFixed />}
          </IconButton>
        </div>
        <div className="map-legend">
          <span>
            <i className="legend-user" /> {t("You")}
          </span>
          <span>
            <i className="legend-shelter" /> {t("Shelter")}
          </span>
          <span>
            <i className="legend-hazard" /> {t("Hazard")}
          </span>
        </div>
      </div>
      <div className="nearest card">
        <div className="nearest-top">
          <span className="icon-tile green">
            <Cross />
          </span>
          <div>
            <p className="eyebrow">
              {t("Nearest shelter")} · 0.8 {t("mi")}
            </p>
            <h2>{t(shelters[0].name)}</h2>
          </div>
          <button className="text-button" onClick={() => setSelected(shelters[0])}>
            {t("Details")}
          </button>
        </div>
        <div className="availability">
          <span>
            <Users /> {shelters[0].availability} {t("spaces available")}
          </span>
          <strong>{t("Open")}</strong>
        </div>
        <button
          className="button primary"
          onClick={() =>
            onToast("Directions preview opened. No external navigation started.", "info")
          }
        >
          <Navigation /> {t("Get directions")}
        </button>
      </div>
      {selected && (
        <div className="sheet-backdrop" onMouseDown={() => setSelected(null)}>
          <div
            className="bottom-sheet"
            onMouseDown={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-label={`${t(selected.name)} ${t("details")}`}
          >
            <span className="sheet-handle" />
            <button
              className="sheet-close"
              onClick={() => setSelected(null)}
              aria-label={t("Close shelter details")}
            >
              <X />
            </button>
            <span className="icon-tile green large">
              <Cross />
            </span>
            <p className="eyebrow">
              {t("Emergency shelter")} · {t("Open")}
            </p>
            <h2>{t(selected.name)}</h2>
            <p className="sheet-address">
              <MapPin /> {selected.address} · {selected.distance}
            </p>
            <div className="capacity-block">
              <div>
                <span>{t("Current availability")}</span>
                <strong>
                  {selected.availability} {t("spaces")}
                </strong>
              </div>
              <div>
                <span>{t("Total capacity")}</span>
                <strong>{selected.capacity}</strong>
              </div>
            </div>
            <h3>{t("Facilities")}</h3>
            <div className="tag-row">
              {selected.facilities.map((f) => (
                <span key={f}>{t(f)}</span>
              ))}
            </div>
            <div className={`medical-callout ${selected.medical ? "" : "muted"}`}>
              <Stethoscope />
              <div>
                <strong>
                  {t(selected.medical ? "Medical aid available" : "No on-site medical aid")}
                </strong>
                <small>
                  {t(
                    selected.medical
                      ? "First-aid staff and basic supplies on site"
                      : "Basic shelter services only",
                  )}
                </small>
              </div>
            </div>
            <button
              className="button primary"
              onClick={() =>
                onToast("Route preview ready. Check local conditions before travel.", "success")
              }
            >
              <Navigation /> {t("Navigate to shelter")}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function FirstAidScreen({ online }: { online: boolean }) {
  const t = useT();
  const [selected, setSelected] = useState<Guide | null>(() => {
    const id = sessionStorage.getItem("sentinel-guide");
    sessionStorage.removeItem("sentinel-guide");
    return guides.find((g) => g.id === id) || null;
  });
  const [query, setQuery] = useState("");
  const filtered = guides.filter((g) =>
    `${g.title} ${g.subtitle} ${t(g.title)} ${t(g.subtitle)}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  if (selected) {
    return (
      <section className="screen guide-detail">
        <Header
          title={t(selected.title)}
          eyebrow="Offline guide"
          online={online}
          onBack={() => setSelected(null)}
        />
        <div className="guide-hero">
          <span>{selected.icon}</span>
          <p>{t("Stay calm. Follow each step in order.")}</p>
        </div>
        <div className="emergency-note">
          <Phone />
          <span>
            <strong>{t("Call emergency services when safe.")}</strong>{" "}
            {t("This guide supports, but does not replace, trained medical care.")}
          </span>
        </div>
        <ol className="steps">
          {selected.steps.map((step, index) => (
            <li key={step}>
              <span>{index + 1}</span>
              <p>{t(step)}</p>
            </li>
          ))}
        </ol>
        <div className="warning-card">
          <AlertTriangle />
          <div>
            <strong>{t("Important")}</strong>
            <p>{t(selected.warning)}</p>
          </div>
        </div>
        <button className="button secondary" onClick={() => setSelected(null)}>
          <ArrowLeft /> {t("Back to all guides")}
        </button>
      </section>
    );
  }
  return (
    <section className="screen">
      <Header title="First aid" eyebrow="Works without internet" online={online} />
      <div className="search-field">
        <Search />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={t("Search guides")}
          aria-label={t("Search first-aid guides")}
        />
        {query && (
          <button onClick={() => setQuery("")} aria-label={t("Clear search")}>
            <X />
          </button>
        )}
      </div>
      <div className="offline-banner">
        <Download />
        <div>
          <strong>{t("7 guides saved offline")}</strong>
          <small>{t("Always available on this device")}</small>
        </div>
        <Check />
      </div>
      <div className="section-heading">
        <div>
          <p className="eyebrow">{t("Emergency procedures")}</p>
          <h2>{t("Choose a guide")}</h2>
        </div>
      </div>
      <div className="guide-list">
        {filtered.map((guide) => (
          <button className="guide-card card" key={guide.id} onClick={() => setSelected(guide)}>
            <span className="guide-icon">{guide.icon}</span>
            <div>
              <strong>{t(guide.title)}</strong>
              <small>
                {t(guide.subtitle)} · {guide.steps.length} {t("steps")}
              </small>
            </div>
            <ChevronRight />
          </button>
        ))}
        {!filtered.length && (
          <div className="empty-state">
            <Search />
            <h3>{t("No guides found")}</h3>
            <p>{t("Try a different search term.")}</p>
          </div>
        )}
      </div>
    </section>
  );
}

type Message = { role: "bot" | "user"; text: string };
function BotScreen({
  online,
  navigate,
  microphonePermission,
  onRequestMicrophone,
}: {
  online: boolean;
  navigate: (screen: Screen) => void;
  microphonePermission: PermissionState;
  onRequestMicrophone: () => Promise<PermissionRequestResult>;
}) {
  const t = useT();
  const language = useContext(LanguageContext);
  const [messages, setMessages] = useState<Message[]>([
    { role: "bot", text: t("I can help you find the right first-aid steps. What is happening?") },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [voiceState, setVoiceState] = useState<VoiceState>("idle");
  const [voiceError, setVoiceError] = useState("");
  const messagesRef = useRef<HTMLDivElement>(null);
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const voiceResultRef = useRef(false);
  const voiceErrorRef = useRef(false);
  useEffect(() => {
    const container = messagesRef.current;
    if (container) container.scrollTop = container.scrollHeight;
  }, [messages, loading]);

  useEffect(
    () => () => {
      recognitionRef.current?.abort();
      recognitionRef.current = null;
    },
    [],
  );

  const stopVoiceInput = () => {
    setVoiceState("processing");
    recognitionRef.current?.stop();
  };

  const startVoiceInput = async () => {
    if (voiceState === "listening" || voiceState === "recording") {
      stopVoiceInput();
      return;
    }

    setVoiceError("");
    voiceResultRef.current = false;
    voiceErrorRef.current = false;
    setVoiceState("listening");
    const permission =
      microphonePermission === "granted"
        ? { state: "granted" as const }
        : await onRequestMicrophone();
    if (permission.state !== "granted") {
      setVoiceState("error");
      setVoiceError(
        permission.state === "denied"
          ? "Microphone permission was denied. Enable it in device settings to use voice input."
          : "Microphone access could not be started. You can continue using text chat.",
      );
      return;
    }

    const voiceWindow = window as typeof window & {
      SpeechRecognition?: SpeechRecognitionConstructor;
      webkitSpeechRecognition?: SpeechRecognitionConstructor;
    };
    const SpeechRecognition = voiceWindow.SpeechRecognition ?? voiceWindow.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setVoiceState("error");
      setVoiceError(
        "Voice recognition is unavailable in this browser. You can continue using text chat.",
      );
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.lang = language === "hi" ? "hi-IN" : "en-US";
    recognition.onaudiostart = () => setVoiceState("recording");
    recognition.onspeechstart = () => setVoiceState("recording");
    recognition.onresult = (event) => {
      voiceResultRef.current = true;
      setVoiceState("processing");
      const transcript = event.results[0]?.[0]?.transcript?.trim() ?? "";
      if (transcript) setInput(transcript);
      else {
        setVoiceState("error");
        setVoiceError("No speech was detected. Try again or type your question.");
      }
    };
    recognition.onerror = () => {
      voiceErrorRef.current = true;
      setVoiceState("error");
      setVoiceError("Voice input could not be processed. You can continue using text chat.");
    };
    recognition.onend = () => {
      recognitionRef.current = null;
      if (voiceErrorRef.current) return;
      if (voiceResultRef.current) {
        window.setTimeout(() => setVoiceState("idle"), 650);
        return;
      }
      setVoiceState("error");
      setVoiceError("No speech was detected. Try again or type your question.");
    };
    recognitionRef.current = recognition;
    try {
      recognition.start();
    } catch {
      recognitionRef.current = null;
      setVoiceState("error");
      setVoiceError("Voice input could not be started. You can continue using text chat.");
    }
  };

  const send = (text = input) => {
    const trimmed = text.trim();
    if (!trimmed || loading || !online) return;
    setInput("");
    setMessages((m) => [...m, { role: "user", text: trimmed }]);
    setLoading(true);
    window.setTimeout(() => {
      const lower = trimmed.toLowerCase();
      let reply = t(
        "Keep the person safe and monitor breathing. If there is immediate danger, call your local emergency number now.",
      );
      if (lower.includes("bleed") || trimmed === t("Severe bleeding"))
        reply = t(
          "Apply firm, direct pressure with a clean cloth. Keep pressure constant and add more dressing on top if needed.",
        );
      if (lower.includes("chok") || trimmed === t("Someone is choking"))
        reply = t(
          "If they cannot speak or cough, give 5 back blows followed by 5 abdominal thrusts. Repeat and call emergency services.",
        );
      if (lower.includes("burn") || trimmed === t("Treat a burn"))
        reply = t(
          "Cool the burn under clean, cool running water for 20 minutes. Do not use ice or creams.",
        );
      if (lower.includes("cpr") || lower.includes("breath") || trimmed === t("Start CPR"))
        reply = t(
          "Check responsiveness and normal breathing. Call emergency services, then begin firm chest compressions at 100–120 per minute.",
        );
      setMessages((m) => [...m, { role: "bot", text: reply }]);
      setLoading(false);
    }, 1000);
  };

  return (
    <section className="screen bot-screen">
      <Header
        title="Aid bot"
        eyebrow={online ? "First-aid guidance" : "Unavailable offline"}
        online={online}
      />
      {!online ? (
        <div className="bot-offline">
          <span>
            <WifiOff />
          </span>
          <h2>{t("Bot needs a connection")}</h2>
          <p>
            {t(
              "Chat is the only feature unavailable offline. Your downloaded first-aid guides are ready.",
            )}
          </p>
          <button className="button primary" onClick={() => navigate("aid")}>
            <Cross /> {t("Open offline guides")}
          </button>
          <div className="offline-options">
            {guides.slice(0, 4).map((guide) => (
              <button
                type="button"
                key={guide.id}
                onClick={() => {
                  sessionStorage.setItem("sentinel-guide", guide.id);
                  navigate("aid");
                }}
              >
                {guide.icon}
                <span>{t(guide.title)}</span>
                <ChevronRight />
              </button>
            ))}
          </div>
        </div>
      ) : (
        <>
          <div className="chat-note">
            <Info /> {t("General guidance only. Not a medical professional.")}
          </div>
          <div className="messages" ref={messagesRef}>
            {messages.map((message, index) => (
              <div className={`message ${message.role}`} key={`${message.text}-${index}`}>
                {message.role === "bot" && (
                  <span>
                    <Bot />
                  </span>
                )}
                <p>{message.text}</p>
              </div>
            ))}
            {loading && (
              <div className="message bot">
                <span>
                  <Bot />
                </span>
                <p className="typing">
                  <i />
                  <i />
                  <i />
                </p>
              </div>
            )}
          </div>
          <div className="quick-prompts">
            {["Severe bleeding", "Someone is choking", "Treat a burn", "Start CPR"].map(
              (prompt) => (
                <button
                  type="button"
                  key={prompt}
                  onClick={() => send(t(prompt))}
                  disabled={loading}
                >
                  {t(prompt)}
                </button>
              ),
            )}
          </div>
          <div className="voice-input-area">
            {voiceState !== "idle" && (
              <div className={`voice-status ${voiceState}`} role="status" aria-live="polite">
                <Mic />
                <span>
                  {t(
                    voiceState === "listening"
                      ? "Listening…"
                      : voiceState === "recording"
                        ? "Recording… Tap stop when finished."
                        : voiceState === "processing"
                          ? "Processing voice input…"
                          : voiceError,
                  )}
                </span>
                {voiceState === "error" && microphonePermission === "denied" && (
                  <button type="button" onClick={() => navigate("settings")}>
                    {t("Open device settings")}
                  </button>
                )}
              </div>
            )}
            <form
              className="chat-input"
              onSubmit={(e) => {
                e.preventDefault();
                send();
              }}
            >
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder={t("Describe the situation…")}
                aria-label={t("Chat message")}
                disabled={loading}
              />
              <button
                type="button"
                className={`voice-button ${
                  voiceState === "listening" || voiceState === "recording" ? "active" : ""
                }`}
                onClick={startVoiceInput}
                disabled={loading || voiceState === "processing"}
                aria-label={t(
                  voiceState === "listening" || voiceState === "recording"
                    ? "Stop voice input"
                    : "Ask with your voice",
                )}
              >
                {voiceState === "listening" || voiceState === "recording" ? <Square /> : <Mic />}
              </button>
              <button
                type="submit"
                disabled={!input.trim() || loading}
                aria-label={t("Send message")}
              >
                {loading ? <span className="spinner" /> : <Send />}
              </button>
            </form>
          </div>
        </>
      )}
    </section>
  );
}

function Toggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: () => void;
  label: string;
}) {
  return (
    <button
      className={`toggle ${checked ? "on" : ""}`}
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={onChange}
    >
      <span />
    </button>
  );
}

function SettingsScreen({
  online,
  setOnline,
  theme,
  setTheme,
  language,
  setLanguage,
  permissionStatuses,
  onRequestPermission,
  onRefreshPermissions,
  navigate,
  onToast,
}: {
  online: boolean;
  setOnline: (v: boolean) => void;
  theme: string;
  setTheme: (v: string) => void;
  language: Language;
  setLanguage: (v: Language) => void;
  permissionStatuses: PermissionStatuses;
  onRequestPermission: (kind: PermissionKind) => Promise<PermissionRequestResult>;
  onRefreshPermissions: () => Promise<void>;
  navigate: (s: Screen) => void;
  onToast: (t: string, type?: "success" | "error" | "info") => void;
}) {
  const t = useT();
  const [notifications, setNotifications] = usePersistedState("sentinel-notifications", true);
  const [locked, setLocked] = usePersistedState("sentinel-medical-lock", false);
  const [biometrics, setBiometrics] = usePersistedState("sentinel-biometrics", false);
  const [pinOpen, setPinOpen] = useState(false);
  const [pin, setPin] = useState("");
  const [permissionBusy, setPermissionBusy] = useState<PermissionKind | "refresh" | null>(null);
  const [permissionGuidance, setPermissionGuidance] = useState<PermissionKind | null>(null);

  const permissionItems: {
    kind: PermissionKind;
    title: string;
    description: string;
    icon: ReactNode;
  }[] = [
    {
      kind: "location",
      title: "Location access",
      description: "Location access is needed to show your position and nearby shelters.",
      icon: <MapPin />,
    },
    {
      kind: "camera",
      title: "Camera access",
      description: "Camera access is needed to scan a Medical ID QR code.",
      icon: <Camera />,
    },
    {
      kind: "microphone",
      title: "Microphone access",
      description: "Microphone access allows voice questions in the First Aid Bot.",
      icon: <Mic />,
    },
    {
      kind: "notifications",
      title: "Notifications access",
      description: "Notifications can show emergency and safety alerts.",
      icon: <Bell />,
    },
    {
      kind: "bluetooth",
      title: "Bluetooth access",
      description: "Bluetooth can connect to nearby SOS relays when supported.",
      icon: <Bluetooth />,
    },
  ];

  const savePin = (e: FormEvent) => {
    e.preventDefault();
    if (pin.length !== 4) return;
    localStorage.setItem(PIN_STORAGE_KEY, pin);
    setPin("");
    setPinOpen(false);
    onToast("PIN saved on this device.");
  };

  const managePermission = async (kind: PermissionKind) => {
    if (permissionStatuses[kind] === "denied") {
      setPermissionGuidance(kind);
      return;
    }
    setPermissionBusy(kind);
    await onRequestPermission(kind);
    setPermissionBusy(null);
  };

  return (
    <section className="screen settings-screen">
      <Header title="Settings" eyebrow="Device preferences" online={online} />
      <div className="settings-profile card">
        <span>
          <UserRound />
        </span>
        <div>
          <strong>Alex Morgan</strong>
          <small>{t("Emergency profile on this device")}</small>
        </div>
        <button onClick={() => navigate("medical")}>{t("Edit")}</button>
      </div>
      <div className="settings-group">
        <p className="eyebrow">{t("General")}</p>
        <label className="setting-row">
          <span className="setting-icon">
            <Languages />
          </span>
          <div>
            <strong>{t("Language")}</strong>
            <small>{t("App display language")}</small>
          </div>
          <select
            value={language}
            onChange={(e) => {
              setLanguage(e.target.value as Language);
              onToast("Language preference saved.", "info");
            }}
            aria-label={t("Language")}
          >
            <option value="en">English</option>
            <option value="hi">हिन्दी (Hindi)</option>
          </select>
        </label>
        <div className="setting-row">
          <span className="setting-icon">
            <Sun />
          </span>
          <div>
            <strong>{t("Theme")}</strong>
            <small>{t(theme === "light" ? "Light mode" : "Dark mode")}</small>
          </div>
          <Toggle
            label={t("Toggle theme")}
            checked={theme === "dark"}
            onChange={() => setTheme(theme === "dark" ? "light" : "dark")}
          />
        </div>
        <div className="setting-row">
          <span className="setting-icon">
            <Bell />
          </span>
          <div>
            <strong>{t("Safety notifications")}</strong>
            <small>{t("Local preparedness reminders")}</small>
          </div>
          <Toggle
            label={t("Toggle notifications")}
            checked={notifications}
            onChange={() => setNotifications(!notifications)}
          />
        </div>
      </div>
      <div className="settings-group">
        <p className="eyebrow">{t("Security & Medical ID")}</p>
        <div className="setting-row">
          <span className="setting-icon">
            <Lock />
          </span>
          <div>
            <strong>{t("Lock Medical ID")}</strong>
            <small>{t("Require unlock before editing")}</small>
          </div>
          <Toggle
            label={t("Lock Medical ID")}
            checked={locked}
            onChange={() => setLocked(!locked)}
          />
        </div>
        <button className="setting-row" onClick={() => setPinOpen(true)}>
          <span className="setting-icon">
            <Smartphone />
          </span>
          <div>
            <strong>{t("Change PIN")}</strong>
            <small>{t(getStoredPin() ? "PIN is set" : "No PIN set")}</small>
          </div>
          <ChevronRight />
        </button>
        <div className="setting-row">
          <span className="setting-icon">
            <Fingerprint />
          </span>
          <div>
            <strong>{t("Biometric unlock")}</strong>
            <small>{t("Use device biometrics to unlock")}</small>
          </div>
          <Toggle
            label={t("Biometric unlock")}
            checked={biometrics}
            onChange={() => {
              setBiometrics(!biometrics);
              onToast(
                !biometrics ? "Biometric unlock enabled." : "Biometric unlock disabled.",
                "info",
              );
            }}
          />
        </div>
      </div>
      <div className="settings-group">
        <div className="settings-group-heading">
          <p className="eyebrow">{t("Permissions")}</p>
          <button
            className="permission-refresh"
            onClick={async () => {
              setPermissionBusy("refresh");
              await onRefreshPermissions();
              setPermissionBusy(null);
              onToast("Permission status refreshed.", "success");
            }}
            disabled={permissionBusy !== null}
          >
            {permissionBusy === "refresh" ? <span className="spinner" /> : <RefreshCw />}
            {t("Refresh")}
          </button>
        </div>
        {permissionItems.map((item) => {
          const status = permissionStatuses[item.kind];
          const statusText =
            status === "granted" ? "Granted" : status === "denied" ? "Denied" : "Not Requested";
          return (
            <div className="setting-row permission-setting-row" key={item.kind}>
              <span className={`setting-icon permission-${status}`}>{item.icon}</span>
              <div>
                <strong>{t(item.title)}</strong>
                <small>{t(item.description)}</small>
                <span className={`permission-status ${status}`}>{t(statusText)}</span>
              </div>
              <button
                className="permission-manage"
                onClick={() => managePermission(item.kind)}
                disabled={permissionBusy !== null}
              >
                {permissionBusy === item.kind ? (
                  <span className="spinner" />
                ) : (
                  t(status === "denied" ? "Open device settings" : "Manage")
                )}
              </button>
            </div>
          );
        })}
      </div>
      <div className="settings-group">
        <p className="eyebrow">{t("Connectivity & offline")}</p>
        <div className="setting-row">
          <span className={`setting-icon ${online ? "green" : "amber"}`}>
            {online ? <Wifi /> : <WifiOff />}
          </span>
          <div>
            <strong>{t("Connectivity setting")}</strong>
            <small>{t(online ? "Bot features enabled" : "Offline mode active")}</small>
          </div>
          <Toggle
            label={t("Toggle connectivity")}
            checked={online}
            onChange={() => setOnline(!online)}
          />
        </div>
        <button
          className="setting-row"
          onClick={() => onToast("Offline map is current. Last checked today.", "success")}
        >
          <span className="setting-icon">
            <MapIcon />
          </span>
          <div>
            <strong>{t("Offline map")}</strong>
            <small>{t("28 MB · Updated today")}</small>
          </div>
          <Check />
        </button>
      </div>
      <div className="settings-group">
        <p className="eyebrow">{t("Information")}</p>
        <button
          className="setting-row"
          onClick={() => onToast("S.E.N.T.I.N.E.L. · Version 1.0", "info")}
        >
          <span className="setting-icon">
            <Info />
          </span>
          <div>
            <strong>{t("About")}</strong>
            <small>Version 1.0.0</small>
          </div>
          <ChevronRight />
        </button>
        <button
          className="setting-row"
          onClick={() => onToast("Your profile stays in this browser’s local storage.", "info")}
        >
          <span className="setting-icon">
            <Shield />
          </span>
          <div>
            <strong>{t("Privacy")}</strong>
            <small>{t("Local data and permissions")}</small>
          </div>
          <ChevronRight />
        </button>
        <button
          className="setting-row"
          onClick={() =>
            onToast("This app is not a replacement for emergency services or medical care.", "info")
          }
        >
          <span className="setting-icon">
            <AlertTriangle />
          </span>
          <div>
            <strong>{t("Safety disclaimer")}</strong>
            <small>{t("Important limitations")}</small>
          </div>
          <ChevronRight />
        </button>
      </div>
      <p className="version-note">S.E.N.T.I.N.E.L. · {t("Preparedness, kept close.")}</p>
      {permissionGuidance && (
        <div className="modal-backdrop">
          <div className="modal card" role="dialog" aria-modal="true">
            <button
              className="sheet-close"
              onClick={() => setPermissionGuidance(null)}
              aria-label={t("Dismiss")}
            >
              <X />
            </button>
            <span className="modal-icon">
              <SettingsIcon />
            </span>
            <h2>{t("Open device settings")}</h2>
            <p>
              {t(
                "Open your browser or device settings, allow access for this site, then return and refresh permissions.",
              )}
            </p>
            <button
              className="button primary"
              onClick={async () => {
                setPermissionBusy("refresh");
                await onRefreshPermissions();
                setPermissionBusy(null);
                setPermissionGuidance(null);
                onToast("Permission status refreshed.", "success");
              }}
            >
              {permissionBusy === "refresh" ? <span className="spinner" /> : <RefreshCw />}
              {t("Refresh permissions")}
            </button>
            <button className="button secondary" onClick={() => setPermissionGuidance(null)}>
              {t("Done")}
            </button>
          </div>
        </div>
      )}
      {pinOpen && (
        <div className="modal-backdrop">
          <form className="modal card" onSubmit={savePin}>
            <button
              type="button"
              className="sheet-close"
              onClick={() => {
                setPinOpen(false);
                setPin("");
              }}
              aria-label={t("Close PIN dialog")}
            >
              <X />
            </button>
            <span className="modal-icon">
              <Lock />
            </span>
            <h2>{t("Set a PIN")}</h2>
            <p>{t("This PIN is stored on this device. Do not use a PIN you use elsewhere.")}</p>
            <input
              autoFocus
              inputMode="numeric"
              maxLength={4}
              value={pin}
              onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
              placeholder="••••"
              aria-label={t("Four-digit PIN")}
            />
            <button className="button primary" type="submit" disabled={pin.length !== 4}>
              {t("Save PIN")}
            </button>
            <button className="button secondary" type="button" onClick={() => setPinOpen(false)}>
              {t("Cancel")}
            </button>
          </form>
        </div>
      )}
    </section>
  );
}

function MedicalScreen({
  online,
  language,
  cameraPermission,
  onCameraPermissionChange,
  onBack,
  onToast,
}: {
  online: boolean;
  language: Language;
  cameraPermission: PermissionState;
  onCameraPermissionChange: (state: PermissionState) => void;
  onBack: () => void;
  onToast: (t: string, type?: "success" | "error" | "info") => void;
}) {
  const t = useT();
  const [saved, setSaved] = usePersistedState<MedicalData>("sentinel-medical-id", initialMedical);
  const [draft, setDraft] = useState(saved);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showQr, setShowQr] = useState(false);
  const [showScanner, setShowScanner] = useState(false);
  const [unlocked, setUnlocked] = useState(
    () => !JSON.parse(localStorage.getItem("sentinel-medical-lock") || "false"),
  );
  const [pin, setPin] = useState("");
  const qrCells = useMemo(
    () => Array.from({ length: 121 }, (_, i) => (i * 17 + i * i * 3 + 7) % 11 < 5),
    [],
  );

  const save = () => {
    if (!draft.name.trim()) {
      onToast("Name is required before saving.", "error");
      return;
    }
    setSaving(true);
    window.setTimeout(() => {
      setSaved(draft);
      setSaving(false);
      setEditing(false);
      onToast("Medical ID saved locally.");
    }, 700);
  };
  const unlockWithPin = () => {
    const stored = getStoredPin();
    if (!stored || pin === stored) {
      setUnlocked(true);
      setPin("");
      onToast("Medical ID unlocked.", "success");
    } else onToast("Incorrect PIN.", "error");
  };
  const importScannedMedicalId = (data: unknown) => {
    if (!data || typeof data !== "object") return;
    const record = data as Partial<Record<keyof MedicalData, unknown>>;
    const next = { ...saved };
    (Object.keys(next) as (keyof MedicalData)[]).forEach((key) => {
      if (typeof record[key] === "string") next[key] = record[key] as string;
    });
    setSaved(next);
    setDraft(next);
    setShowScanner(false);
    onToast("Medical ID imported with your consent.", "success");
  };
  if (showScanner) {
    return (
      <QrScanner
        language={language}
        onClose={() => setShowScanner(false)}
        onDecoded={importScannedMedicalId}
        cameraPermission={cameraPermission}
        onPermissionChange={onCameraPermissionChange}
      />
    );
  }
  if (!unlocked) {
    return (
      <section className="screen">
        <Header title="Medical ID" eyebrow="Protected on device" online={online} onBack={onBack} />
        <div className="locked-state">
          <span>
            <Lock />
          </span>
          <h2>{t("Medical ID locked")}</h2>
          <p>{t("Enter your 4-digit PIN or use biometric unlock.")}</p>
          <input
            inputMode="numeric"
            maxLength={4}
            value={pin}
            onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
            placeholder="••••"
            aria-label={t("PIN")}
          />
          <button className="button primary" disabled={pin.length !== 4} onClick={unlockWithPin}>
            <Lock /> {t("Unlock")}
          </button>
          <button
            className="button secondary"
            onClick={() => {
              setUnlocked(true);
              onToast("Medical ID unlocked.", "info");
            }}
          >
            <Fingerprint /> {t("Use biometric unlock")}
          </button>
        </div>
      </section>
    );
  }
  const fields: { key: keyof MedicalData; label: string; placeholder: string; wide?: boolean }[] = [
    { key: "name", label: "Full name", placeholder: "Your name" },
    { key: "age", label: "Age", placeholder: "Age" },
    { key: "blood", label: "Blood group", placeholder: "Unknown" },
    { key: "allergies", label: "Allergies", placeholder: "None known", wide: true },
    { key: "conditions", label: "Medical conditions", placeholder: "None known", wide: true },
    { key: "medications", label: "Medications", placeholder: "None", wide: true },
    { key: "contactName", label: "Emergency contact", placeholder: "Contact name" },
    { key: "contactPhone", label: "Contact phone", placeholder: "Phone number" },
    { key: "notes", label: "Additional notes", placeholder: "Important details", wide: true },
  ];
  return (
    <section className="screen medical-screen">
      <Header
        title="Medical ID"
        eyebrow="Saved on this device"
        online={online}
        onBack={onBack}
        action={
          <button
            className="header-action"
            onClick={() => (editing ? save() : setEditing(true))}
            disabled={saving}
          >
            {saving ? <span className="spinner" /> : t(editing ? "Save" : "Edit")}
          </button>
        }
      />
      <div className="medical-hero">
        <span>
          <CircleUserRound />
        </span>
        <div>
          <h2>{saved.name || t("Your Medical ID")}</h2>
          <p>{t("Emergency information · Offline available")}</p>
        </div>
        <strong>{saved.blood}</strong>
      </div>
      <div className="medical-info">
        <Info />{" "}
        {t("First responders may look for Medical ID information. Keep it accurate and concise.")}
      </div>
      <div className="medical-form">
        {fields.map((field) => (
          <label className={field.wide ? "wide" : ""} key={field.key}>
            <span>{t(field.label)}</span>
            {field.key === "notes" ? (
              <textarea
                disabled={!editing}
                value={draft[field.key]}
                placeholder={t(field.placeholder)}
                onChange={(e) => setDraft({ ...draft, [field.key]: e.target.value })}
              />
            ) : (
              <input
                disabled={!editing}
                value={draft[field.key]}
                placeholder={t(field.placeholder)}
                onChange={(e) => setDraft({ ...draft, [field.key]: e.target.value })}
              />
            )}
          </label>
        ))}
      </div>
      {editing && (
        <div className="edit-actions">
          <button className="button primary" onClick={save} disabled={saving}>
            {saving ? (
              <>
                <span className="spinner" /> {t("Saving…")}
              </>
            ) : (
              <>
                <Check /> {t("Save locally")}
              </>
            )}
          </button>
          <button
            className="button secondary"
            onClick={() => {
              setDraft(saved);
              setEditing(false);
            }}
          >
            {t("Cancel changes")}
          </button>
        </div>
      )}
      {!editing && (
        <div className="medical-qr-actions">
          <button className="qr-share card" onClick={() => setShowScanner(true)}>
            <span>
              <Camera />
            </span>
            <div>
              <strong>{t("Open QR scanner")}</strong>
              <small>{t("Scan a Medical ID QR code")}</small>
            </div>
            <ChevronRight />
          </button>
          <button className="qr-share card" onClick={() => setShowQr(true)}>
            <span>
              <QrCode />
            </span>
            <div>
              <strong>{t("Share emergency QR")}</strong>
              <small>{t("Show your Medical ID summary")}</small>
            </div>
            <ChevronRight />
          </button>
        </div>
      )}
      {showQr && (
        <div className="modal-backdrop">
          <div className="modal qr-modal card">
            <button
              className="sheet-close"
              onClick={() => setShowQr(false)}
              aria-label={t("Close QR")}
            >
              <X />
            </button>
            <p className="eyebrow">{t("Emergency Medical ID")}</p>
            <h2>{saved.name}</h2>
            <div className="qr-grid" aria-label={t("QR code")}>
              {qrCells.map((active, i) => (
                <i className={active ? "on" : ""} key={i} />
              ))}
            </div>
            <p>{t("This visual is not a standardized medical QR code.")}</p>
            <button className="button secondary" onClick={() => setShowQr(false)}>
              {t("Done")}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function PermissionOnboarding({
  statuses,
  onRequest,
  onComplete,
}: {
  statuses: PermissionStatuses;
  onRequest: (kind: PermissionKind) => Promise<PermissionRequestResult>;
  onComplete: () => void;
}) {
  const t = useT();
  const [busy, setBusy] = useState<PermissionKind | null>(null);
  const items: { kind: PermissionKind; text: string; icon: ReactNode }[] = [
    {
      kind: "location",
      text: "Location: user location and nearby shelters.",
      icon: <MapPin />,
    },
    { kind: "camera", text: "Camera: scan Medical ID QR codes.", icon: <Camera /> },
    {
      kind: "microphone",
      text: "Microphone: ask voice questions in the First Aid Bot.",
      icon: <Mic />,
    },
    {
      kind: "notifications",
      text: "Notifications: emergency and safety alerts.",
      icon: <Bell />,
    },
    {
      kind: "bluetooth",
      text: "Bluetooth: nearby SOS relay connectivity.",
      icon: <Bluetooth />,
    },
  ];

  return (
    <section className="permission-onboarding" aria-label={t("Permission onboarding")}>
      <div className="permission-onboarding-card">
        <span className="onboarding-shield">
          <Shield />
        </span>
        <p className="eyebrow">{t("Set up permissions")}</p>
        <h1>{t("Choose what this app may access")}</h1>
        <p className="onboarding-copy">
          {t(
            "Allow access only when asked for a feature. You can skip now and change access later in Settings.",
          )}
        </p>
        <div className="onboarding-permissions">
          {items.map((item) => {
            const status = statuses[item.kind];
            const statusText =
              status === "granted" ? "Granted" : status === "denied" ? "Denied" : "Not Requested";
            return (
              <div className="onboarding-permission" key={item.kind}>
                <span>{item.icon}</span>
                <div>
                  <strong>{t(item.text)}</strong>
                  <small className={`permission-status ${status}`}>{t(statusText)}</small>
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    setBusy(item.kind);
                    await onRequest(item.kind);
                    setBusy(null);
                  }}
                  disabled={busy !== null || status === "granted"}
                >
                  {busy === item.kind ? (
                    <span className="spinner" />
                  ) : status === "granted" ? (
                    <Check />
                  ) : (
                    t("Request")
                  )}
                </button>
              </div>
            );
          })}
        </div>
        <button className="button primary" onClick={onComplete}>
          {t("Continue")}
        </button>
        <button className="button secondary" onClick={onComplete}>
          {t("Skip")}
        </button>
      </div>
    </section>
  );
}

function BottomNav({ active, navigate }: { active: Screen; navigate: (s: Screen) => void }) {
  const t = useT();
  const items = [
    { screen: "home" as Screen, label: "Home", icon: <Home /> },
    { screen: "map" as Screen, label: "Map", icon: <MapIcon /> },
    { screen: "aid" as Screen, label: "First Aid", icon: <Cross /> },
    { screen: "bot" as Screen, label: "Bot", icon: <Bot /> },
    { screen: "settings" as Screen, label: "Settings", icon: <SettingsIcon /> },
  ];
  return (
    <nav className="bottom-nav" aria-label={t("Primary navigation")}>
      {items.map((item) => (
        <button
          key={item.screen}
          className={
            active === item.screen || (active === "medical" && item.screen === "settings")
              ? "active"
              : ""
          }
          onClick={() => navigate(item.screen)}
        >
          {item.icon}
          <span>{t(item.label)}</span>
        </button>
      ))}
    </nav>
  );
}

export default function App() {
  const [screen, setScreen] = useState<Screen>("home");
  const [online, setOnline] = usePersistedState("sentinel-online", true);
  const [theme, setTheme] = usePersistedState("sentinel-theme", "light");
  const [storedLanguage, setStoredLanguage] = usePersistedState<string>("sentinel-language", "en");
  const language: Language = storedLanguage === "hi" ? "hi" : "en";
  const [onboardingComplete, setOnboardingComplete] = usePersistedState(
    "sentinel-permission-onboarding-complete",
    false,
  );
  const [permissionStatuses, setPermissionStatuses] = useState<PermissionStatuses>({
    location: "prompt",
    camera: "prompt",
    microphone: "prompt",
    notifications: "prompt",
    bluetooth: "prompt",
  });
  const [toast, setToast] = useState<{ text: string; type: "success" | "error" | "info" } | null>(
    null,
  );
  const showToast = (text: string, type: "success" | "error" | "info" = "success") =>
    setToast({ text, type });
  const refreshPermissionState = async () => {
    setPermissionStatuses(await refreshPermissions());
  };
  const requestPermissionAccess = async (kind: PermissionKind) => {
    const result = await requestPermission(kind);
    setPermissionStatuses((current) => ({ ...current, [kind]: result.state }));
    if (result.state === "granted") showToast("Permission granted.", "success");
    else if (result.state === "denied")
      showToast("Permission denied. You can enable it in device settings.", "error");
    else showToast("Permission request was not completed.", "info");
    return result;
  };
  useEffect(() => {
    refreshPermissionState();
    const refreshOnFocus = () => refreshPermissionState();
    window.addEventListener("focus", refreshOnFocus);
    return () => window.removeEventListener("focus", refreshOnFocus);
  }, []);
  const navigate = (next: Screen) => {
    setScreen(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  return (
    <LanguageContext.Provider value={language}>
      <div className={`app-shell ${theme}`} lang={language === "hi" ? "hi" : "en-US"}>
        {!onboardingComplete && (
          <PermissionOnboarding
            statuses={permissionStatuses}
            onRequest={requestPermissionAccess}
            onComplete={() => setOnboardingComplete(true)}
          />
        )}
        {onboardingComplete && (
          <>
            <div className="desktop-brand">
              <span className="brand-mark">
                <Shield />
              </span>
              <div>
                <strong>S.E.N.T.I.N.E.L.</strong>
                <small>{translate(language, "Emergency readiness system")}</small>
              </div>
            </div>
            <main>
              {screen === "home" && (
                <HomeScreen online={online} navigate={navigate} onToast={showToast} />
              )}
              {screen === "map" && <MapScreen online={online} onToast={showToast} />}
              {screen === "aid" && <FirstAidScreen online={online} />}
              {screen === "bot" && (
                <BotScreen
                  online={online}
                  navigate={navigate}
                  microphonePermission={permissionStatuses.microphone}
                  onRequestMicrophone={() => requestPermissionAccess("microphone")}
                />
              )}
              {screen === "settings" && (
                <SettingsScreen
                  online={online}
                  setOnline={setOnline}
                  theme={theme}
                  setTheme={setTheme}
                  language={language}
                  setLanguage={setStoredLanguage}
                  permissionStatuses={permissionStatuses}
                  onRequestPermission={requestPermissionAccess}
                  onRefreshPermissions={refreshPermissionState}
                  navigate={navigate}
                  onToast={showToast}
                />
              )}
              {screen === "medical" && (
                <MedicalScreen
                  online={online}
                  language={language}
                  cameraPermission={permissionStatuses.camera}
                  onCameraPermissionChange={(state) =>
                    setPermissionStatuses((current) => ({ ...current, camera: state }))
                  }
                  onBack={() => navigate("home")}
                  onToast={showToast}
                />
              )}
            </main>
            <BottomNav active={screen} navigate={navigate} />
          </>
        )}
        {toast && <Toast text={toast.text} type={toast.type} onClose={() => setToast(null)} />}
      </div>
    </LanguageContext.Provider>
  );
}
