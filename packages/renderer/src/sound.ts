/**
 * The synth sound bank — every sound generated from oscillators, envelopes, and
 * noise. No audio assets exist anywhere in Junction (same philosophy as the
 * procedural card art: agents author parameters, not binaries).
 *
 * AudioContext starts lazily on the first user gesture (browser policy); plays
 * before that are silently skipped. Everything is wrapped defensively so test
 * DOMs and ancient browsers degrade to silence, never to errors.
 */

export type SoundSetName = "off" | "soft" | "arcade";

export type SoundName =
  | "start"
  | "tap"
  | "place"
  | "flip"
  | "winTrick"
  | "loseTrick"
  | "match"
  | "mismatch"
  | "fanfare"
  | "defeat";

export interface SoundBank {
  /** Call from a user-gesture handler to unlock audio. Safe to call repeatedly. */
  readonly unlock: () => void;
  readonly play: (name: SoundName) => void;
  readonly setMuted: (muted: boolean) => void;
  readonly isMuted: () => boolean;
}

interface Voice {
  readonly type: OscillatorType;
  readonly from: number;
  readonly to?: number;
  readonly at: number;
  readonly duration: number;
  readonly gain: number;
}

interface NoiseVoice {
  readonly at: number;
  readonly duration: number;
  readonly gain: number;
  readonly filterFrom: number;
  readonly filterTo: number;
}

interface Patch {
  readonly voices: readonly Voice[];
  readonly noise?: NoiseVoice;
}

const N = {
  C5: 523.25, E5: 659.25, G5: 783.99, C6: 1046.5, A4: 440, F4: 349.23, D4: 293.66, G4: 392,
};

function patches(set: Exclude<SoundSetName, "off">): Record<SoundName, Patch> {
  const soft = set === "soft";
  const wave: OscillatorType = soft ? "sine" : "square";
  const wave2: OscillatorType = soft ? "triangle" : "sawtooth";
  const g = soft ? 1 : 0.72; // arcade waves are louder per-sample; trim
  return {
    start: {
      voices: [
        { type: wave2, from: N.C5, at: 0, duration: 0.12, gain: 0.12 * g },
        { type: wave2, from: N.E5, at: 0.07, duration: 0.12, gain: 0.12 * g },
        { type: wave2, from: N.G5, at: 0.14, duration: 0.18, gain: 0.12 * g },
      ],
    },
    tap: { voices: [{ type: wave, from: 620, to: 480, at: 0, duration: 0.06, gain: 0.16 * g }] },
    place: {
      voices: [{ type: wave, from: 300, to: 180, at: 0, duration: 0.1, gain: 0.1 * g }],
      noise: { at: 0, duration: 0.12, gain: 0.1, filterFrom: 2400, filterTo: 500 },
    },
    flip: { voices: [{ type: wave2, from: 320, to: 720, at: 0, duration: 0.09, gain: 0.14 * g }] },
    winTrick: {
      voices: [
        { type: wave, from: N.E5, at: 0, duration: 0.1, gain: 0.14 * g },
        { type: wave, from: N.G5, at: 0.09, duration: 0.16, gain: 0.14 * g },
      ],
    },
    loseTrick: { voices: [{ type: wave, from: N.A4, to: N.F4, at: 0, duration: 0.16, gain: 0.09 * g }] },
    match: {
      voices: [
        { type: wave, from: N.C5, at: 0, duration: 0.08, gain: 0.13 * g },
        { type: wave, from: N.E5, at: 0.07, duration: 0.08, gain: 0.13 * g },
        { type: wave, from: N.G5, at: 0.14, duration: 0.2, gain: 0.13 * g },
      ],
    },
    mismatch: { voices: [{ type: soft ? "triangle" : "square", from: 180, to: 140, at: 0, duration: 0.16, gain: 0.1 * g }] },
    fanfare: {
      voices: [
        { type: wave2, from: N.C5, at: 0, duration: 0.14, gain: 0.13 * g },
        { type: wave2, from: N.E5, at: 0.12, duration: 0.14, gain: 0.13 * g },
        { type: wave2, from: N.G5, at: 0.24, duration: 0.14, gain: 0.13 * g },
        { type: wave2, from: N.C6, at: 0.36, duration: 0.34, gain: 0.15 * g },
        { type: wave, from: N.C6 * 1.005, at: 0.36, duration: 0.34, gain: 0.07 * g },
      ],
    },
    defeat: {
      voices: [
        { type: wave2, from: N.G4, at: 0, duration: 0.18, gain: 0.1 * g },
        { type: wave2, from: N.D4, at: 0.16, duration: 0.3, gain: 0.1 * g },
      ],
    },
  };
}

export function createSoundBank(set: SoundSetName, initiallyMuted = false): SoundBank {
  let context: AudioContext | undefined;
  let noiseBuffer: AudioBuffer | undefined;
  let muted = initiallyMuted;

  const supported = (): boolean => typeof window !== "undefined" && "AudioContext" in window;

  function unlock(): void {
    if (set === "off" || muted || !supported()) return;
    try {
      context ??= new AudioContext();
      if (context.state === "suspended") void context.resume();
    } catch {
      context = undefined;
    }
  }

  function getNoise(ctx: AudioContext): AudioBuffer {
    if (noiseBuffer === undefined) {
      noiseBuffer = ctx.createBuffer(1, ctx.sampleRate * 0.3, ctx.sampleRate);
      const data = noiseBuffer.getChannelData(0);
      for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
    }
    return noiseBuffer;
  }

  function play(name: SoundName): void {
    if (set === "off" || muted || context === undefined || context.state !== "running") return;
    try {
      const patch = patches(set)[name];
      const now = context.currentTime + 0.01;
      for (const voice of patch.voices) {
        const osc = context.createOscillator();
        const gain = context.createGain();
        osc.type = voice.type;
        osc.frequency.setValueAtTime(voice.from, now + voice.at);
        if (voice.to !== undefined)
          osc.frequency.exponentialRampToValueAtTime(Math.max(voice.to, 1), now + voice.at + voice.duration);
        gain.gain.setValueAtTime(0, now + voice.at);
        gain.gain.linearRampToValueAtTime(voice.gain, now + voice.at + 0.012);
        gain.gain.exponentialRampToValueAtTime(0.0008, now + voice.at + voice.duration);
        osc.connect(gain).connect(context.destination);
        osc.start(now + voice.at);
        osc.stop(now + voice.at + voice.duration + 0.05);
      }
      if (patch.noise !== undefined) {
        const source = context.createBufferSource();
        source.buffer = getNoise(context);
        const filter = context.createBiquadFilter();
        filter.type = "bandpass";
        filter.frequency.setValueAtTime(patch.noise.filterFrom, now + patch.noise.at);
        filter.frequency.exponentialRampToValueAtTime(patch.noise.filterTo, now + patch.noise.at + patch.noise.duration);
        const gain = context.createGain();
        gain.gain.setValueAtTime(patch.noise.gain, now + patch.noise.at);
        gain.gain.exponentialRampToValueAtTime(0.0008, now + patch.noise.at + patch.noise.duration);
        source.connect(filter).connect(gain).connect(context.destination);
        source.start(now + patch.noise.at);
        source.stop(now + patch.noise.at + patch.noise.duration + 0.05);
      }
    } catch {
      // Silence is always acceptable; errors never are.
    }
  }

  return {
    unlock,
    play,
    setMuted: (value: boolean) => {
      muted = value;
      if (!muted) unlock();
    },
    isMuted: () => muted,
  };
}

/** Map a game event to a sound, from the viewer's perspective. */
export function soundForEvent(
  event: { type: string; [key: string]: unknown },
  viewerSeat: number,
): SoundName | null {
  switch (event.type) {
    case "gameStarted":
      return "start";
    case "pieceMoved":
      return event["revealed"] !== undefined ? "place" : null;
    case "pieceFlipped":
      return event["faceUp"] === true ? "flip" : null;
    case "zoneResolved":
      if (event["winnerSeat"] === null) return null;
      return event["winnerSeat"] === viewerSeat ? "winTrick" : "loseTrick";
    case "pairResolved":
      return event["matched"] === true ? "match" : "mismatch";
    case "gameEnded":
      return event["winnerSeat"] === viewerSeat ? "fanfare" : "defeat";
    default:
      return null;
  }
}
