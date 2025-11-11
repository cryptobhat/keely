#!/usr/bin/env python3
"""
Generate bubble sound effects for keyboard
Creates short, pleasant "pop" sounds for key presses
"""

import struct
import math
import os

def generate_bubble_sound(filename, duration_ms=50, frequency=800, decay=True):
    """
    Generate a bubble-like sound effect

    Args:
        filename: Output WAV file path
        duration_ms: Duration in milliseconds
        frequency: Base frequency in Hz
        decay: Whether to add exponential decay (bubble effect)
    """
    sample_rate = 44100  # CD quality
    num_samples = int(sample_rate * duration_ms / 1000)

    # WAV file header
    wav_header = struct.pack('<4sI4s4sIHHIIHH4sI',
        b'RIFF',
        36 + num_samples * 2,  # File size - 8
        b'WAVE',
        b'fmt ',
        16,  # fmt chunk size
        1,   # Audio format (1 = PCM)
        1,   # Number of channels (mono)
        sample_rate,
        sample_rate * 2,  # Byte rate
        2,   # Block align
        16,  # Bits per sample
        b'data',
        num_samples * 2  # Data chunk size
    )

    # Generate audio samples
    samples = []
    for i in range(num_samples):
        t = i / sample_rate

        # Multiple frequency components for richer "bubble" sound
        wave = 0.3 * math.sin(2 * math.pi * frequency * t)
        wave += 0.2 * math.sin(2 * math.pi * frequency * 1.5 * t)  # Harmonic
        wave += 0.1 * math.sin(2 * math.pi * frequency * 2 * t)    # Second harmonic

        # Exponential decay for bubble "pop" effect
        if decay:
            envelope = math.exp(-5 * t / (duration_ms / 1000))
            wave *= envelope

        # Attack envelope (fade in at start to avoid click)
        attack_samples = int(sample_rate * 0.005)  # 5ms attack
        if i < attack_samples:
            wave *= i / attack_samples

        # Convert to 16-bit integer
        sample = int(wave * 32767 * 0.7)  # 70% volume to avoid clipping
        sample = max(-32768, min(32767, sample))
        samples.append(struct.pack('<h', sample))

    # Write WAV file
    with open(filename, 'wb') as f:
        f.write(wav_header)
        f.write(b''.join(samples))

    print(f"Generated: {filename} ({duration_ms}ms, {frequency}Hz)")

def main():
    output_dir = r"C:\Users\Nags\AndroidStudioProjects\Kavi\app\src\main\res\raw"
    os.makedirs(output_dir, exist_ok=True)

    print("Generating bubble sound effects for keyboard...")

    # Standard key - medium pitch bubble
    generate_bubble_sound(
        os.path.join(output_dir, "key_click.wav"),
        duration_ms=40,
        frequency=850,
        decay=True
    )

    # Delete key - lower pitch, slightly longer
    generate_bubble_sound(
        os.path.join(output_dir, "key_delete.wav"),
        duration_ms=45,
        frequency=650,
        decay=True
    )

    # Space key - softer, lower pitch
    generate_bubble_sound(
        os.path.join(output_dir, "key_space.wav"),
        duration_ms=50,
        frequency=700,
        decay=True
    )

    # Enter key - higher pitch, more prominent
    generate_bubble_sound(
        os.path.join(output_dir, "key_enter.wav"),
        duration_ms=55,
        frequency=950,
        decay=True
    )

    # Modifier key - short, subtle
    generate_bubble_sound(
        os.path.join(output_dir, "key_modifier.wav"),
        duration_ms=35,
        frequency=800,
        decay=True
    )

    print("\nAll bubble sounds generated successfully!")
    print(f"Location: {output_dir}")
    print("\nNote: WAV files generated. Android prefers OGG format.")
    print("To convert to OGG, use ffmpeg or Android Studio will handle it automatically.")

if __name__ == "__main__":
    main()
