#!/usr/bin/env python3
"""
Builds a 10,000-word Kannada dictionary file from the open Alar Kannada-English
lexicon (https://github.com/alar-dict/data, OdBL license).

The script:
1. Downloads/caches the upstream `alar.yml` data if needed.
2. Parses every `entry:` headword, keeping only pure Kannada tokens.
3. Uses the number of dictionary senses per word as a crude popularity score.
4. Preserves the existing starter words (the curated 600 we already ship) at the top.
5. Writes `app/src/main/assets/dictionaries/kannada_dictionary.txt`
   with the requested number of high-frequency everyday words.

Run:
    python -X utf8 tools/dictionaries/generate_kannada_dictionary.py \
        --target-count 10000
"""

from __future__ import annotations

import argparse
import math
import re
import sys
import textwrap
import urllib.request
from collections import Counter
from pathlib import Path
from typing import Iterable, List, Sequence

REPO_ROOT = Path(__file__).resolve().parents[2]
ASSET_PATH = (
    REPO_ROOT / "app" / "src" / "main" / "assets" / "dictionaries" / "kannada_dictionary.txt"
)

ALAR_URL = "https://raw.githubusercontent.com/alar-dict/data/master/alar.yml"
CACHE_DIR = REPO_ROOT / "tools" / "dictionaries" / ".cache"
CACHE_FILE = CACHE_DIR / "alar.yml"

ENTRY_PATTERN = re.compile(r"^\s*entry:\s*(.+)$")
KANNADA_RANGE = ("\u0c80", "\u0cff")


def ensure_source_file(path: Path, url: str) -> Path:
    if path.exists():
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    print(f"Downloading {url} -> {path}")
    with urllib.request.urlopen(url) as response, path.open("wb") as fh:
        chunk = response.read(1024 * 512)
        while chunk:
            fh.write(chunk)
            chunk = response.read(1024 * 512)
    return path


def clean_existing_words(path: Path) -> List[str]:
    if not path.exists():
        return []
    words: List[str] = []
    seen = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        parts = stripped.rsplit(" ", 1)
        if len(parts) != 2:
            continue
        word = parts[0].strip()
        if not word:
            continue
        if any((ch < KANNADA_RANGE[0]) or (ch > KANNADA_RANGE[1]) for ch in word):
            continue
        if len(word) < 2:
            continue
        if word in seen:
            continue
        seen.add(word)
        words.append(word)
    return words


def extract_word(raw: str) -> str | None:
    # Remove YAML quoting if present.
    if raw.startswith(("'", '"')) and raw.endswith(("'", '"')):
        raw = raw[1:-1]
    raw = raw.strip().replace("\u200c", "").replace("\u200d", "")
    if not raw:
        return None

    chars: List[str] = []
    for ch in raw:
        if KANNADA_RANGE[0] <= ch <= KANNADA_RANGE[1]:
            chars.append(ch)
        else:
            break
    word = "".join(chars).strip()
    if len(word) < 2:
        return None
    return word


def load_word_counts(path: Path) -> Counter[str]:
    counter: Counter[str] = Counter()
    with path.open("r", encoding="utf-8") as fh:
        for line in fh:
            match = ENTRY_PATTERN.match(line)
            if not match:
                continue
            word = extract_word(match.group(1))
            if not word:
                continue
            counter[word] += 1
    return counter


def filter_ranked_words(
    counter: Counter[str],
    banned: set[str],
    keep: int,
    min_len: int,
    max_len: int,
) -> List[str]:
    candidates = []
    for word, count in counter.items():
        if word in banned:
            continue
        if len(word) < min_len or len(word) > max_len:
            continue
        candidates.append((word, count))

    candidates.sort(key=lambda item: (-item[1], len(item[0]), item[0]))
    return [word for word, _ in candidates[:keep]]


def assign_frequencies(words: Sequence[str], start: int, minimum: int) -> List[int]:
    if not words:
        return []
    if len(words) == 1:
        return [start]
    freq_span = start - minimum
    freq_values: List[int] = []
    for idx, _ in enumerate(words):
        frac = idx / (len(words) - 1)
        freq = round(start - frac * freq_span)
        freq = max(minimum, freq)
        freq_values.append(freq)
    return freq_values


def write_dictionary(words: Sequence[str], freqs: Sequence[int], path: Path, total_words: int) -> None:
    header = textwrap.dedent(
        f"""\
        # Kannada Dictionary
        # Format: word frequency
        # Auto-generated via tools/dictionaries/generate_kannada_dictionary.py
        # Source: Alar Kannada-English Dictionary (OdBL) — https://github.com/alar-dict/data
        # Total entries: {total_words}
        """
    )
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        fh.write(header + "\n")
        for word, freq in zip(words, freqs):
            fh.write(f"{word} {freq}\n")


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate a large Kannada dictionary file.")
    parser.add_argument("--target-count", type=int, default=10000, help="Total number of words to output.")
    parser.add_argument(
        "--min-length",
        type=int,
        default=2,
        help="Minimum character length for candidate words.",
    )
    parser.add_argument(
        "--max-length",
        type=int,
        default=16,
        help="Maximum character length for candidate words.",
    )
    parser.add_argument(
        "--start-frequency",
        type=int,
        default=5000,
        help="Frequency assigned to the first (most common) word.",
    )
    parser.add_argument(
        "--min-frequency",
        type=int,
        default=200,
        help="Floor frequency for the tail of the list.",
    )
    parser.add_argument(
        "--seed-limit",
        type=int,
        default=600,
        help="How many existing starter words to pin to the top (0 to disable).",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])

    source_path = ensure_source_file(CACHE_FILE, ALAR_URL)
    print(f"Loaded Alar dataset from {source_path}")
    counter = load_word_counts(source_path)
    print(f"Extracted {len(counter):,} unique Kannada tokens from Alar")

    existing_words = clean_existing_words(ASSET_PATH)
    seed_words = existing_words[: args.seed_limit]
    seed_set = {word for word in seed_words if len(word) >= args.min_length}

    required = args.target_count - len(seed_set)
    if required <= 0:
        print("Target count is less than or equal to seed words; consider lowering --seed-limit.")
        required = 0

    ranked_words = filter_ranked_words(
        counter=counter,
        banned=seed_set,
        keep=max(required, 0),
        min_len=args.min_length,
        max_len=args.max_length,
    )

    total_words = seed_words + [word for word in ranked_words if word not in seed_set]
    total_words = total_words[: args.target_count]
    freqs = assign_frequencies(total_words, args.start_frequency, args.min_frequency)
    write_dictionary(total_words, freqs, ASSET_PATH, len(total_words))

    print(f"Wrote {len(total_words)} words to {ASSET_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
